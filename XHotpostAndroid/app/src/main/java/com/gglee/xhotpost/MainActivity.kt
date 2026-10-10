package com.gglee.xhotpost

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.gglee.xhotpost.domain.AppSettings
import com.gglee.xhotpost.domain.Draft
import com.gglee.xhotpost.domain.DraftStatus
import com.gglee.xhotpost.domain.HotTopic
import com.gglee.xhotpost.work.SyncWorker
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    private lateinit var container: AppContainer
    private lateinit var txtStats: TextView
    private lateinit var txtBanner: TextView
    private lateinit var txtStatus: TextView
    private lateinit var content: LinearLayout
    private lateinit var btnTick: Button

    private var settings: AppSettings = AppSettings()
    private var drafts: List<Draft> = emptyList()
    private var topics: List<HotTopic> = emptyList()
    private var tab = Tab.REVIEW
    private var collectJob: Job? = null

    private enum class Tab { REVIEW, TOPICS, SETTINGS }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, true)

        val previousCrash = CrashLogger.consume(this)
        if (previousCrash != null) {
            setContentView(buildCrashView(previousCrash))
            return
        }

        try {
            CrashLogger.install(applicationContext)
            container = (application as HotpostApplication).container
            setContentView(R.layout.activity_main)
            bindViews()
            startCollectors()
            lifecycleScope.launch {
                try {
                    val current = withContext(Dispatchers.IO) {
                        container.repository.settings.first()
                    }
                    SyncWorker.schedule(applicationContext, current.pollIntervalMinutes.toLong())
                } catch (t: Throwable) {
                    Log.e(TAG, "schedule failed", t)
                }
                runTick(silent = true)
            }
        } catch (t: Throwable) {
            Log.e(TAG, "onCreate failed", t)
            CrashLogger.save(this, t)
            setContentView(buildCrashView(t.stackTraceToString()))
        }
    }

    private fun bindViews() {
        txtStats = findViewById(R.id.txtStats)
        txtBanner = findViewById(R.id.txtBanner)
        txtStatus = findViewById(R.id.txtStatus)
        btnTick = findViewById(R.id.btnTick)
        val frame = findViewById<android.widget.FrameLayout>(R.id.content)
        content = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
        }
        val scroll = ScrollView(this).apply { addView(content) }
        frame.addView(
            scroll,
            android.widget.FrameLayout.LayoutParams(
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
                android.widget.FrameLayout.LayoutParams.MATCH_PARENT,
            ),
        )

        btnTick.setOnClickListener {
            lifecycleScope.launch { runTick(silent = false) }
        }
        findViewById<Button>(R.id.tabReview).setOnClickListener {
            tab = Tab.REVIEW
            render()
        }
        findViewById<Button>(R.id.tabTopics).setOnClickListener {
            tab = Tab.TOPICS
            render()
        }
        findViewById<Button>(R.id.tabSettings).setOnClickListener {
            tab = Tab.SETTINGS
            render()
        }
    }

    private fun startCollectors() {
        collectJob?.cancel()
        collectJob = lifecycleScope.launch {
            combine(
                container.repository.settings,
                container.repository.stats,
                container.repository.drafts,
                container.repository.topics,
            ) { s, stats, d, t ->
                Quad(s, stats, d, t)
            }.collectLatest { q ->
                settings = q.settings
                drafts = q.drafts
                topics = q.topics
                txtStats.text =
                    "待审核 ${q.stats.pendingReview} · 已发布 ${q.stats.published} · 草稿 ${q.stats.draftsCreated}"
                txtBanner.text = if (settings.demoMode) {
                    "演示模式：不会打开 X。关闭后审核通过会跳转 X 发帖（无 API）。"
                } else {
                    "非演示：审核通过后打开 X，文案已填好，你点发送即可。"
                }
                render()
            }
        }
    }

    private data class Quad(
        val settings: AppSettings,
        val stats: com.gglee.xhotpost.domain.DashboardStats,
        val drafts: List<Draft>,
        val topics: List<HotTopic>,
    )

    private fun render() {
        content.removeAllViews()
        when (tab) {
            Tab.REVIEW -> renderReview()
            Tab.TOPICS -> renderTopics()
            Tab.SETTINGS -> renderSettings()
        }
    }

    private fun renderReview() {
        val pending = drafts.filter { it.status == DraftStatus.PENDING_REVIEW }
        val recent = drafts.filter { it.status != DraftStatus.PENDING_REVIEW }.take(8)
        if (pending.isEmpty()) {
            content.addView(simpleText("暂无待审草稿。点「跑一轮」生成。"))
        } else {
            pending.forEach { draft -> content.addView(draftView(draft, editable = true)) }
        }
        if (recent.isNotEmpty()) {
            content.addView(simpleText("最近处理", bold = true))
            recent.forEach { draft -> content.addView(draftView(draft, editable = false)) }
        }
    }

    private fun renderTopics() {
        if (topics.isEmpty()) {
            content.addView(simpleText("暂无热点，先跑一轮。"))
            return
        }
        val inflater = LayoutInflater.from(this)
        topics.take(20).forEach { topic ->
            val view = inflater.inflate(R.layout.item_topic, content, false)
            view.findViewById<TextView>(R.id.txtTitle).text = topic.title
            view.findViewById<TextView>(R.id.txtMeta).text =
                "${topic.source} · 热度 ${topic.score}"
            content.addView(view)
        }
    }

    private fun renderSettings() {
        val view = LayoutInflater.from(this).inflate(R.layout.panel_settings, content, false)
        val editName = view.findViewById<EditText>(R.id.editDisplayName)
        val editAffiliate = view.findViewById<EditText>(R.id.editAffiliate)
        val editCta = view.findViewById<EditText>(R.id.editCta)
        val switchDemo = view.findViewById<Switch>(R.id.switchDemo)
        val switchAutoDraft = view.findViewById<Switch>(R.id.switchAutoDraft)
        val switchAutoPublish = view.findViewById<Switch>(R.id.switchAutoPublish)

        editName.setText(settings.displayName)
        editAffiliate.setText(settings.affiliateUrl)
        editCta.setText(settings.ctaTemplate)
        switchDemo.isChecked = settings.demoMode
        switchAutoDraft.isChecked = settings.autoDraft
        switchAutoPublish.isChecked = settings.autoPublishApproved

        view.findViewById<Button>(R.id.btnSaveSettings).setOnClickListener {
            val next = settings.copy(
                displayName = editName.text.toString().ifBlank { "热帖" },
                affiliateUrl = editAffiliate.text.toString().trim(),
                ctaTemplate = editCta.text.toString().ifBlank { settings.ctaTemplate },
                demoMode = switchDemo.isChecked,
                autoDraft = switchAutoDraft.isChecked,
                autoPublishApproved = switchAutoPublish.isChecked,
            )
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    container.repository.updateSettings { next }
                }
                toast("设置已保存")
            }
        }
        content.addView(view)
    }

    private fun draftView(draft: Draft, editable: Boolean): View {
        val view = LayoutInflater.from(this).inflate(R.layout.item_draft, content, false)
        view.findViewById<TextView>(R.id.txtTitle).text =
            if (editable) draft.topicTitle else "${draft.topicTitle} · ${draft.status.name}"
        val edit = view.findViewById<EditText>(R.id.editText)
        edit.setText(draft.text)
        edit.isEnabled = editable
        view.findViewById<TextView>(R.id.txtCount).text = "${draft.text.length}/280"
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val btnReject = view.findViewById<Button>(R.id.btnReject)
        val btnApprove = view.findViewById<Button>(R.id.btnApprove)
        if (!editable) {
            btnSave.visibility = View.GONE
            btnReject.visibility = View.GONE
            btnApprove.visibility = View.GONE
            return view
        }
        btnSave.setOnClickListener {
            val text = edit.text.toString()
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    container.repository.saveDraftText(draft.id, text)
                }
                toast("已保存")
            }
        }
        btnReject.setOnClickListener {
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    container.repository.rejectDraft(draft.id)
                }
                toast("已拒绝")
            }
        }
        btnApprove.setOnClickListener {
            val text = edit.text.toString()
            lifecycleScope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        container.repository.approveDraft(draft.id, text)
                    }
                    toast(
                        when {
                            result.status == DraftStatus.PUBLISHED && result.demo ->
                                "已通过（演示发布）"
                            result.status == DraftStatus.PUBLISHED ->
                                "已打开 X，请确认发送"
                            result.status == DraftStatus.FAILED ->
                                "失败：${result.publishError ?: "未知"}"
                            else -> "已通过"
                        },
                    )
                } catch (t: Throwable) {
                    toast(t.message ?: "发布失败")
                }
            }
        }
        return view
    }

    private suspend fun runTick(silent: Boolean) {
        btnTick.isEnabled = false
        txtStatus.text = "正在抓热点 / 写草稿…"
        try {
            val result = withContext(Dispatchers.IO) { container.repository.runTick() }
            val msg = "完成：热点 ${result.topics} · 新草稿 ${result.drafts} · 发布 ${result.published}"
            txtStatus.text = msg
            if (!silent) toast(msg)
        } catch (t: Throwable) {
            Log.e(TAG, "tick failed", t)
            txtStatus.text = "失败：${t.message}"
            if (!silent) toast(t.message ?: "失败")
        } finally {
            btnTick.isEnabled = true
        }
    }

    private fun simpleText(text: String, bold: Boolean = false): TextView {
        return TextView(this).apply {
            this.text = text
            setTextColor(0xFF9BB3AA.toInt())
            textSize = if (bold) 16f else 14f
            setPadding(0, 12, 0, 12)
        }
    }

    private fun toast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }

    private fun buildCrashView(message: String): View {
        val scroll = ScrollView(this).apply {
            setBackgroundColor(0xFF071411.toInt())
            setPadding(48, 96, 48, 48)
        }
        val text = TextView(this).apply {
            setTextColor(0xFFFFB4B4.toInt())
            textSize = 13f
            this.text = "热帖启动失败，请截图发给开发者：\n\n$message"
        }
        scroll.addView(text)
        return scroll
    }

    companion object {
        private const val TAG = "HotpostMain"
    }
}
