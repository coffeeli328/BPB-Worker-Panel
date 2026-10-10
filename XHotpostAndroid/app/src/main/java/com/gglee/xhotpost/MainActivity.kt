package com.gglee.xhotpost

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import com.gglee.xhotpost.domain.AppSettings
import com.gglee.xhotpost.domain.ContentLanguage
import com.gglee.xhotpost.domain.Draft
import com.gglee.xhotpost.domain.DraftStatus
import com.gglee.xhotpost.domain.HotTopic
import com.gglee.xhotpost.domain.NicheId
import com.gglee.xhotpost.domain.WritingStyle
import com.gglee.xhotpost.domain.XPublisher
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

    private val loginLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult(),
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val name = result.data?.getStringExtra(XLoginActivity.EXTRA_USERNAME).orEmpty()
            toast(if (name.isBlank()) "X 登录状态已更新" else "已登录：$name")
            // force refresh from store
            lifecycleScope.launch {
                settings = withContext(Dispatchers.IO) {
                    container.repository.settings.first()
                }
                txtBanner.text = buildBanner(settings)
                if (tab == Tab.SETTINGS) render()
            }
        }
    }

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
                txtBanner.text = buildBanner(settings)
                render()
            }
        }
    }

    private fun buildBanner(s: AppSettings): String {
        val niche = nicheLabel(s)
        val login = if (s.xLoggedIn) {
            "X 已登录${s.xUsername.takeIf { it.isNotBlank() }?.let { "($it)" } ?: ""}"
        } else {
            "X 未登录（请到设置登录）"
        }
        return if (s.demoMode) {
            "演示模式 · 话题：$niche · $login"
        } else {
            "正式发帖 · 话题：$niche · $login"
        }
    }

    private fun nicheLabel(s: AppSettings): String = when (s.niche) {
        NicheId.TECH -> "科技/AI"
        NicheId.FINANCE -> "财经"
        NicheId.LIFESTYLE -> "生活"
        NicheId.CREATOR -> "自媒体"
        NicheId.LOCAL -> "综合"
        NicheId.CUSTOM -> s.customNicheLabel.ifBlank { "自定义" }
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
        val txtXStatus = view.findViewById<TextView>(R.id.txtXStatus)
        val editXUsername = view.findViewById<EditText>(R.id.editXUsername)
        val switchXLoggedIn = view.findViewById<Switch>(R.id.switchXLoggedIn)
        val groupNiche = view.findViewById<RadioGroup>(R.id.groupNiche)
        val editCustomNiche = view.findViewById<EditText>(R.id.editCustomNiche)
        val groupLanguage = view.findViewById<RadioGroup>(R.id.groupLanguage)
        val groupStyle = view.findViewById<RadioGroup>(R.id.groupStyle)
        val editPersona = view.findViewById<EditText>(R.id.editPersona)
        val switchAi = view.findViewById<Switch>(R.id.switchAi)
        val editAiBaseUrl = view.findViewById<EditText>(R.id.editAiBaseUrl)
        val editAiModel = view.findViewById<EditText>(R.id.editAiModel)
        val editAiApiKey = view.findViewById<EditText>(R.id.editAiApiKey)
        val editName = view.findViewById<EditText>(R.id.editDisplayName)
        val editAffiliate = view.findViewById<EditText>(R.id.editAffiliate)
        val editCta = view.findViewById<EditText>(R.id.editCta)
        val switchDemo = view.findViewById<Switch>(R.id.switchDemo)
        val switchAutoDraft = view.findViewById<Switch>(R.id.switchAutoDraft)
        val switchAutoPublish = view.findViewById<Switch>(R.id.switchAutoPublish)

        txtXStatus.text = if (settings.xLoggedIn) {
            "状态：已登录${settings.xUsername.takeIf { it.isNotBlank() }?.let { " · $it" } ?: ""}"
        } else {
            "状态：未登录（发帖前请先登录 X）"
        }
        editXUsername.setText(settings.xUsername)
        switchXLoggedIn.isChecked = settings.xLoggedIn

        when (settings.niche) {
            NicheId.TECH -> view.findViewById<RadioButton>(R.id.nicheTech).isChecked = true
            NicheId.FINANCE -> view.findViewById<RadioButton>(R.id.nicheFinance).isChecked = true
            NicheId.LIFESTYLE -> view.findViewById<RadioButton>(R.id.nicheLifestyle).isChecked = true
            NicheId.CREATOR -> view.findViewById<RadioButton>(R.id.nicheCreator).isChecked = true
            NicheId.LOCAL -> view.findViewById<RadioButton>(R.id.nicheLocal).isChecked = true
            NicheId.CUSTOM -> view.findViewById<RadioButton>(R.id.nicheCustom).isChecked = true
        }
        editCustomNiche.setText(settings.customNicheLabel)
        editCustomNiche.visibility =
            if (settings.niche == NicheId.CUSTOM) View.VISIBLE else View.GONE
        groupNiche.setOnCheckedChangeListener { _, checkedId ->
            editCustomNiche.visibility =
                if (checkedId == R.id.nicheCustom) View.VISIBLE else View.GONE
        }

        when (settings.language) {
            ContentLanguage.ZH -> view.findViewById<RadioButton>(R.id.langZh).isChecked = true
            ContentLanguage.EN -> view.findViewById<RadioButton>(R.id.langEn).isChecked = true
            ContentLanguage.MIXED -> view.findViewById<RadioButton>(R.id.langMixed).isChecked = true
        }

        when (settings.writingStyle) {
            WritingStyle.OPINION -> view.findViewById<RadioButton>(R.id.styleOpinion).isChecked = true
            WritingStyle.HOWTO -> view.findViewById<RadioButton>(R.id.styleHowto).isChecked = true
            WritingStyle.STORY -> view.findViewById<RadioButton>(R.id.styleStory).isChecked = true
            WritingStyle.CASUAL -> view.findViewById<RadioButton>(R.id.styleCasual).isChecked = true
            WritingStyle.PRO -> view.findViewById<RadioButton>(R.id.stylePro).isChecked = true
        }
        editPersona.setText(settings.persona)
        switchAi.isChecked = settings.aiEnabled
        editAiBaseUrl.setText(settings.aiBaseUrl)
        editAiModel.setText(settings.aiModel)
        editAiApiKey.setText(settings.aiApiKey)

        editName.setText(settings.displayName)
        editAffiliate.setText(settings.affiliateUrl)
        editCta.setText(settings.ctaTemplate)
        switchDemo.isChecked = settings.demoMode
        switchAutoDraft.isChecked = settings.autoDraft
        switchAutoPublish.isChecked = settings.autoPublishApproved

        view.findViewById<Button>(R.id.btnLoginX).setOnClickListener {
            loginLauncher.launch(android.content.Intent(this, XLoginActivity::class.java))
        }
        view.findViewById<Button>(R.id.btnOpenXApp).setOnClickListener {
            if (!XPublisher.openXApp(this)) {
                toast("未找到 X App，请先安装或用网页登录")
            }
        }

        view.findViewById<Button>(R.id.btnSaveSettings).setOnClickListener {
            val niche = when (groupNiche.checkedRadioButtonId) {
                R.id.nicheFinance -> NicheId.FINANCE
                R.id.nicheLifestyle -> NicheId.LIFESTYLE
                R.id.nicheCreator -> NicheId.CREATOR
                R.id.nicheLocal -> NicheId.LOCAL
                R.id.nicheCustom -> NicheId.CUSTOM
                else -> NicheId.TECH
            }
            val language = when (groupLanguage.checkedRadioButtonId) {
                R.id.langEn -> ContentLanguage.EN
                R.id.langMixed -> ContentLanguage.MIXED
                else -> ContentLanguage.ZH
            }
            val writingStyle = when (groupStyle.checkedRadioButtonId) {
                R.id.styleHowto -> WritingStyle.HOWTO
                R.id.styleStory -> WritingStyle.STORY
                R.id.styleCasual -> WritingStyle.CASUAL
                R.id.stylePro -> WritingStyle.PRO
                else -> WritingStyle.OPINION
            }
            val next = settings.copy(
                displayName = editName.text.toString().ifBlank { "热帖" },
                niche = niche,
                customNicheLabel = editCustomNiche.text.toString().trim(),
                language = language,
                writingStyle = writingStyle,
                persona = editPersona.text.toString().ifBlank { AppSettings().persona },
                aiEnabled = switchAi.isChecked,
                aiBaseUrl = editAiBaseUrl.text.toString().trim()
                    .ifBlank { AppSettings().aiBaseUrl },
                aiModel = editAiModel.text.toString().trim()
                    .ifBlank { AppSettings().aiModel },
                aiApiKey = editAiApiKey.text.toString().trim(),
                affiliateUrl = editAffiliate.text.toString().trim(),
                ctaTemplate = editCta.text.toString().ifBlank { settings.ctaTemplate },
                demoMode = switchDemo.isChecked,
                autoDraft = switchAutoDraft.isChecked,
                autoPublishApproved = switchAutoPublish.isChecked,
                xLoggedIn = switchXLoggedIn.isChecked,
                xUsername = editXUsername.text.toString().trim(),
            )
            lifecycleScope.launch {
                withContext(Dispatchers.IO) {
                    container.repository.updateSettings { next }
                }
                toast("设置已保存。点「跑一轮」或「重写待审草稿」")
                tab = Tab.REVIEW
                render()
            }
        }
        view.findViewById<Button>(R.id.btnRegenDrafts).setOnClickListener {
            lifecycleScope.launch {
                val n = withContext(Dispatchers.IO) {
                    container.repository.updateSettings { cur ->
                        cur.copy(
                            writingStyle = when (groupStyle.checkedRadioButtonId) {
                                R.id.styleHowto -> WritingStyle.HOWTO
                                R.id.styleStory -> WritingStyle.STORY
                                R.id.styleCasual -> WritingStyle.CASUAL
                                R.id.stylePro -> WritingStyle.PRO
                                else -> WritingStyle.OPINION
                            },
                            persona = editPersona.text.toString()
                                .ifBlank { AppSettings().persona },
                            aiEnabled = switchAi.isChecked,
                            aiBaseUrl = editAiBaseUrl.text.toString().trim()
                                .ifBlank { AppSettings().aiBaseUrl },
                            aiModel = editAiModel.text.toString().trim()
                                .ifBlank { AppSettings().aiModel },
                            aiApiKey = editAiApiKey.text.toString().trim(),
                        )
                    }
                    container.repository.regeneratePendingDrafts()
                }
                toast(if (n == 0) "没有待审草稿" else "已重写 $n 条待审草稿")
                tab = Tab.REVIEW
                render()
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
            if (!settings.demoMode && !settings.xLoggedIn) {
                toast("请先到「设置」登录 X 账号")
                tab = Tab.SETTINGS
                render()
                return@setOnClickListener
            }
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
