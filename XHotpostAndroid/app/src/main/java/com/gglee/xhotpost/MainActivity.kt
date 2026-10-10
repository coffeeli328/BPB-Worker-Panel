package com.gglee.xhotpost

import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
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
import androidx.browser.customtabs.CustomTabsIntent
import androidx.core.view.WindowCompat
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.lifecycleScope
import com.gglee.xhotpost.domain.AiDraftClient
import com.gglee.xhotpost.domain.AppSettings
import com.gglee.xhotpost.domain.ContentLanguage
import com.gglee.xhotpost.domain.Draft
import com.gglee.xhotpost.domain.DraftGenerator
import com.gglee.xhotpost.domain.DraftStatus
import com.gglee.xhotpost.domain.HotTopic
import com.gglee.xhotpost.domain.NicheId
import com.gglee.xhotpost.domain.ParsedXLink
import com.gglee.xhotpost.domain.WritingStyle
import com.gglee.xhotpost.domain.XLinkParser
import com.gglee.xhotpost.domain.XPublisher
import com.gglee.xhotpost.domain.XTrendRegion
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
    private lateinit var txtVersion: TextView
    private lateinit var content: LinearLayout
    private lateinit var btnTick: Button

    private var settings: AppSettings = AppSettings()
    private var drafts: List<Draft> = emptyList()
    private var topics: List<HotTopic> = emptyList()
    private var aiStatusLine: String = "AI：未使用"
    private var tab = Tab.REVIEW
    private var collectJob: Job? = null
    private var pendingShareInput: String? = null
    private var shareParsed: ParsedXLink? = null

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

    private enum class Tab { REVIEW, TOPICS, SHARE, SETTINGS }

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
            consumeShareIntent(intent)
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consumeShareIntent(intent)
        if (pendingShareInput != null) {
            tab = Tab.SHARE
            render()
        }
    }

    private fun consumeShareIntent(intent: Intent?) {
        if (intent == null) return
        val shared = when (intent.action) {
            Intent.ACTION_SEND -> {
                if (intent.type?.startsWith("text/") == true) {
                    intent.getStringExtra(Intent.EXTRA_TEXT)
                } else {
                    null
                }
            }
            Intent.ACTION_VIEW -> intent.data?.toString()
            else -> null
        }?.trim().orEmpty()
        if (shared.isNotBlank() && XLinkParser.extract(shared) != null) {
            pendingShareInput = shared
            tab = Tab.SHARE
        }
    }

    private fun bindViews() {
        txtStats = findViewById(R.id.txtStats)
        txtBanner = findViewById(R.id.txtBanner)
        txtStatus = findViewById(R.id.txtStatus)
        txtVersion = findViewById(R.id.txtVersion)
        txtVersion.text = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})"
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
        findViewById<Button>(R.id.tabShare).setOnClickListener {
            tab = Tab.SHARE
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
                container.repository.lastAiStatus,
            ) { s, stats, d, t, ai ->
                Quint(s, stats, d, t, ai)
            }.collectLatest { q ->
                settings = q.settings
                drafts = q.drafts
                topics = q.topics
                aiStatusLine = q.aiStatus
                txtStats.text =
                    "待审核 ${q.stats.pendingReview} · 已发布 ${q.stats.published} · 草稿 ${q.stats.draftsCreated}"
                txtBanner.text = buildBanner(settings)
                if (tab != Tab.SETTINGS && tab != Tab.SHARE) {
                    // Avoid wiping in-progress edits on every store tick.
                    render()
                }
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
        val region = when (s.xTrendRegion) {
            XTrendRegion.AUTO -> "自动地区"
            XTrendRegion.UNITED_STATES -> "美国热搜"
            XTrendRegion.UNITED_KINGDOM -> "英国热搜"
            XTrendRegion.JAPAN -> "日本热搜"
            XTrendRegion.SINGAPORE -> "新加坡热搜"
            XTrendRegion.INDIA -> "印度热搜"
        }
        val ai = when {
            !s.aiEnabled || s.aiApiKey.isBlank() -> "AI关"
            AiDraftClient.looksLikeDeepSeekMisconfig(s) -> "AI配置可疑"
            else -> "AI开·${AiDraftClient.resolveModel(s)}"
        }
        val mode = if (s.demoMode) "演示" else "正式"
        return "$mode · v${BuildConfig.VERSION_NAME} · $ai · X热搜 · $region · $niche · $login"
    }

    private fun nicheLabel(s: AppSettings): String = when (s.niche) {
        NicheId.TECH -> "科技/AI"
        NicheId.FINANCE -> "财经"
        NicheId.LIFESTYLE -> "生活"
        NicheId.CREATOR -> "自媒体"
        NicheId.LOCAL -> "综合"
        NicheId.CUSTOM -> s.customNicheLabel.ifBlank { "自定义" }
    }

    private data class Quint(
        val settings: AppSettings,
        val stats: com.gglee.xhotpost.domain.DashboardStats,
        val drafts: List<Draft>,
        val topics: List<HotTopic>,
        val aiStatus: String,
    )

    private fun render() {
        content.removeAllViews()
        when (tab) {
            Tab.REVIEW -> renderReview()
            Tab.TOPICS -> renderTopics()
            Tab.SHARE -> renderShareLink()
            Tab.SETTINGS -> renderSettings()
        }
    }

    private fun renderShareLink() {
        val view = LayoutInflater.from(this).inflate(R.layout.panel_share_link, content, false)
        val editUrl = view.findViewById<EditText>(R.id.editShareUrl)
        val editCaption = view.findViewById<EditText>(R.id.editShareCaption)
        val txtMeta = view.findViewById<TextView>(R.id.txtShareMeta)
        val txtPreview = view.findViewById<TextView>(R.id.txtSharePreview)
        val txtCount = view.findViewById<TextView>(R.id.txtShareCount)

        fun refreshPreview() {
            val parsed = shareParsed ?: XLinkParser.extract(editUrl.text.toString())
            if (parsed?.statusId == null) {
                txtPreview.text = "完整发帖预览会显示在这里"
                txtCount.text = "0/280"
                txtMeta.text = ""
                return
            }
            shareParsed = parsed
            txtMeta.text = "已识别：${parsed.shortLabel}"
            val full = DraftGenerator.linkSharePost(
                parsed,
                editCaption.text.toString(),
                settings,
            )
            txtPreview.text = full
            txtCount.text = "${full.length}/280"
        }

        pendingShareInput?.let {
            editUrl.setText(it)
            shareParsed = XLinkParser.extract(it)
            pendingShareInput = null
            refreshPreview()
        }
        shareParsed?.let {
            if (editUrl.text.isNullOrBlank()) editUrl.setText(it.canonicalUrl)
        }

        editUrl.doAfterTextChanged {
            shareParsed = XLinkParser.extract(it?.toString().orEmpty())
            refreshPreview()
        }
        editCaption.doAfterTextChanged { refreshPreview() }

        view.findViewById<Button>(R.id.btnPasteClipboard).setOnClickListener {
            val clip = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val text = clip.primaryClip?.getItemAt(0)?.coerceToText(this)?.toString().orEmpty()
            if (text.isBlank()) {
                toast("剪贴板为空")
                return@setOnClickListener
            }
            val parsed = XLinkParser.extract(text)
            if (parsed == null) {
                editUrl.setText(text)
                toast("未识别到 X 帖子链接，请检查")
            } else {
                editUrl.setText(parsed.canonicalUrl)
                shareParsed = parsed
                toast("已粘贴：${parsed.shortLabel}")
            }
            refreshPreview()
        }

        view.findViewById<Button>(R.id.btnOpenOriginal).setOnClickListener {
            val parsed = shareParsed ?: XLinkParser.extract(editUrl.text.toString())
            if (parsed == null) {
                toast("请先粘贴有效链接")
                return@setOnClickListener
            }
            openUrl(parsed.canonicalUrl)
        }

        view.findViewById<Button>(R.id.btnGenShareCaption).setOnClickListener {
            lifecycleScope.launch {
                txtStatus.text = "正在生成推荐文案…"
                try {
                    val prep = withContext(Dispatchers.IO) {
                        container.repository.prepareLinkShare(editUrl.text.toString())
                    }
                    shareParsed = prep.parsed
                    editUrl.setText(prep.parsed.canonicalUrl)
                    editCaption.setText(prep.caption)
                    refreshPreview()
                    val msg = when {
                        prep.usedAi -> "已用 AI 生成文案"
                        prep.aiError != null -> "AI 失败（${prep.aiError}），已用模板"
                        else -> "已生成模板文案"
                    }
                    txtStatus.text = msg
                    toast(msg)
                } catch (t: Throwable) {
                    txtStatus.text = t.message
                    toast(t.message ?: "生成失败")
                }
            }
        }

        view.findViewById<Button>(R.id.btnShareToReview).setOnClickListener {
            lifecycleScope.launch {
                try {
                    val prep = withContext(Dispatchers.IO) {
                        container.repository.prepareLinkShare(
                            editUrl.text.toString(),
                            captionOverride = editCaption.text.toString(),
                        )
                    }
                    shareParsed = prep.parsed
                    withContext(Dispatchers.IO) {
                        container.repository.saveLinkShareDraft(prep.fullText, prep.parsed)
                    }
                    toast("已加入待审")
                    tab = Tab.REVIEW
                    render()
                } catch (t: Throwable) {
                    toast(t.message ?: "保存失败")
                }
            }
        }

        view.findViewById<Button>(R.id.btnSharePublish).setOnClickListener {
            if (!settings.demoMode && !settings.xLoggedIn) {
                toast("请先到「设置」登录 X")
                tab = Tab.SETTINGS
                render()
                return@setOnClickListener
            }
            lifecycleScope.launch {
                try {
                    val prep = withContext(Dispatchers.IO) {
                        container.repository.prepareLinkShare(
                            editUrl.text.toString(),
                            captionOverride = editCaption.text.toString(),
                        )
                    }
                    shareParsed = prep.parsed
                    editCaption.setText(prep.caption)
                    refreshPreview()
                    if (settings.demoMode) {
                        withContext(Dispatchers.IO) {
                            container.repository.publishLinkShareNow(prep.fullText, prep.parsed)
                        }
                        toast("演示模式：已记为发布")
                        tab = Tab.REVIEW
                        render()
                    } else {
                        withContext(Dispatchers.IO) {
                            container.repository.saveLinkShareDraft(prep.fullText, prep.parsed)
                        }
                        val opened = XPublisher.openCompose(this@MainActivity, prep.fullText)
                        if (opened) {
                            toast("已打开 X，请确认发送")
                            tab = Tab.REVIEW
                            render()
                        } else {
                            toast("无法打开 X，请安装 X App 或浏览器")
                        }
                    }
                } catch (t: Throwable) {
                    toast(t.message ?: "发布失败")
                }
            }
        }

        refreshPreview()
        content.addView(view)
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
            content.addView(simpleText("暂无 X 热搜，先跑一轮。"))
            return
        }
        content.addView(simpleText("来自 X 平台热搜（点标题打开 X 实时搜索）", bold = true))
        val inflater = LayoutInflater.from(this)
        topics.take(30).forEach { topic ->
            val view = inflater.inflate(R.layout.item_topic, content, false)
            view.findViewById<TextView>(R.id.txtTitle).text = topic.title
            view.findViewById<TextView>(R.id.txtMeta).text =
                "${topic.source} · 热度 ${topic.score}"
            view.setOnClickListener {
                val link = topic.url
                    ?: com.gglee.xhotpost.domain.TrendCollector.xSearchUrl(topic.title)
                openUrl(link)
            }
            content.addView(view)
        }
    }

    private fun openUrl(url: String) {
        try {
            CustomTabsIntent.Builder().build().launchUrl(this, Uri.parse(url))
        } catch (_: Exception) {
            try {
                startActivity(
                    android.content.Intent(
                        android.content.Intent.ACTION_VIEW,
                        Uri.parse(url),
                    ),
                )
            } catch (_: Exception) {
                toast("无法打开链接")
            }
        }
    }

    private fun renderSettings() {
        val view = LayoutInflater.from(this).inflate(R.layout.panel_settings, content, false)
        val txtXStatus = view.findViewById<TextView>(R.id.txtXStatus)
        val editXUsername = view.findViewById<EditText>(R.id.editXUsername)
        val switchXLoggedIn = view.findViewById<Switch>(R.id.switchXLoggedIn)
        val groupXRegion = view.findViewById<RadioGroup>(R.id.groupXRegion)
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

        when (settings.xTrendRegion) {
            XTrendRegion.AUTO -> view.findViewById<RadioButton>(R.id.regionAuto).isChecked = true
            XTrendRegion.UNITED_STATES -> view.findViewById<RadioButton>(R.id.regionUs).isChecked = true
            XTrendRegion.UNITED_KINGDOM -> view.findViewById<RadioButton>(R.id.regionUk).isChecked = true
            XTrendRegion.JAPAN -> view.findViewById<RadioButton>(R.id.regionJp).isChecked = true
            XTrendRegion.SINGAPORE -> view.findViewById<RadioButton>(R.id.regionSg).isChecked = true
            XTrendRegion.INDIA -> view.findViewById<RadioButton>(R.id.regionIn).isChecked = true
        }

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
        val txtAiStatus = view.findViewById<TextView>(R.id.txtAiStatus)
        txtAiStatus.text = aiStatusLine

        view.findViewById<Button>(R.id.btnPresetDeepseek).setOnClickListener {
            editAiBaseUrl.setText(AiDraftClient.DEEPSEEK_BASE)
            editAiModel.setText(AiDraftClient.DEEPSEEK_MODEL)
            switchAi.isChecked = true
            toast("已填入 DeepSeek 地址与模型，请粘贴 Key 后保存")
        }
        view.findViewById<Button>(R.id.btnPresetOpenai).setOnClickListener {
            editAiBaseUrl.setText(AiDraftClient.OPENAI_BASE)
            editAiModel.setText("gpt-4o-mini")
            switchAi.isChecked = true
            toast("已填入 OpenAI 地址与模型，请粘贴 Key 后保存")
        }
        view.findViewById<Button>(R.id.btnTestAi).setOnClickListener {
            lifecycleScope.launch {
                // Persist current AI fields first so test uses what you see.
                val key = editAiApiKey.text.toString().trim()
                val probe = settings.copy(
                    aiEnabled = switchAi.isChecked || key.isNotBlank(),
                    aiBaseUrl = editAiBaseUrl.text.toString().trim()
                        .ifBlank { AiDraftClient.DEEPSEEK_BASE },
                    aiModel = editAiModel.text.toString().trim()
                        .ifBlank { AiDraftClient.DEEPSEEK_MODEL },
                    aiApiKey = key,
                )
                withContext(Dispatchers.IO) {
                    container.repository.updateSettings { probe }
                }
                txtAiStatus.text = "AI：测试中…"
                val msg = withContext(Dispatchers.IO) { container.repository.testAi() }
                txtAiStatus.text = "AI：$msg"
                toast(msg)
            }
        }

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
            val xTrendRegion = when (groupXRegion.checkedRadioButtonId) {
                R.id.regionUs -> XTrendRegion.UNITED_STATES
                R.id.regionUk -> XTrendRegion.UNITED_KINGDOM
                R.id.regionJp -> XTrendRegion.JAPAN
                R.id.regionSg -> XTrendRegion.SINGAPORE
                R.id.regionIn -> XTrendRegion.INDIA
                else -> XTrendRegion.AUTO
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
            val apiKey = editAiApiKey.text.toString().trim()
            val next = settings.copy(
                displayName = editName.text.toString().ifBlank { "热帖" },
                niche = niche,
                customNicheLabel = editCustomNiche.text.toString().trim(),
                language = language,
                xTrendRegion = xTrendRegion,
                writingStyle = writingStyle,
                persona = editPersona.text.toString().ifBlank { AppSettings().persona },
                // Auto-enable AI when a key is present — common DeepSeek setup miss.
                aiEnabled = switchAi.isChecked || apiKey.isNotBlank(),
                aiBaseUrl = editAiBaseUrl.text.toString().trim()
                    .ifBlank { AiDraftClient.DEEPSEEK_BASE },
                aiModel = editAiModel.text.toString().trim()
                    .ifBlank { AiDraftClient.DEEPSEEK_MODEL },
                aiApiKey = apiKey,
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
                val hint = when {
                    apiKey.isBlank() -> "已保存。未填 AI Key，仍用本地模板"
                    AiDraftClient.looksLikeDeepSeekMisconfig(next) ->
                        "已保存。Key 可能是 DeepSeek，但地址仍是 OpenAI — 请点「填入 DeepSeek」"
                    else -> "已保存。请点「重写待审草稿」或「换一版」才会用 AI 更新旧草稿"
                }
                toast(hint)
                tab = Tab.REVIEW
                render()
            }
        }
        view.findViewById<Button>(R.id.btnRegenDrafts).setOnClickListener {
            lifecycleScope.launch {
                txtStatus.text = "正在用 AI/模板重写待审草稿…"
                val result = withContext(Dispatchers.IO) {
                    val apiKey = editAiApiKey.text.toString().trim()
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
                            aiEnabled = switchAi.isChecked || apiKey.isNotBlank(),
                            aiBaseUrl = editAiBaseUrl.text.toString().trim()
                                .ifBlank { AiDraftClient.DEEPSEEK_BASE },
                            aiModel = editAiModel.text.toString().trim()
                                .ifBlank { AiDraftClient.DEEPSEEK_MODEL },
                            aiApiKey = apiKey,
                        )
                    }
                    container.repository.regeneratePendingDrafts()
                }
                val msg = when {
                    result.updated == 0 -> "没有待审草稿"
                    result.aiError != null ->
                        "重写 ${result.updated} 条，AI 成功 ${result.aiOk}；失败：${result.aiError}"
                    else -> "重写 ${result.updated} 条，其中 AI ${result.aiOk} 条"
                }
                txtStatus.text = msg
                toast(msg)
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
        val btnRewrite = view.findViewById<Button>(R.id.btnRewrite)
        val btnSave = view.findViewById<Button>(R.id.btnSave)
        val btnReject = view.findViewById<Button>(R.id.btnReject)
        val btnApprove = view.findViewById<Button>(R.id.btnApprove)
        if (!editable) {
            btnRewrite.visibility = View.GONE
            btnSave.visibility = View.GONE
            btnReject.visibility = View.GONE
            btnApprove.visibility = View.GONE
            return view
        }
        btnRewrite.setOnClickListener {
            lifecycleScope.launch {
                txtStatus.text = "换一版中…"
                val result = withContext(Dispatchers.IO) {
                    container.repository.regenerateDraft(draft.id)
                }
                if (result == null) {
                    toast("无法重写")
                } else {
                    edit.setText(result.text)
                    view.findViewById<TextView>(R.id.txtCount).text = "${result.text.length}/280"
                    val msg = when {
                        result.usedAi -> "已用 AI 换一版"
                        result.aiError != null -> "AI 失败（${result.aiError}），已用模板"
                        else -> "已换一版（本地模板）"
                    }
                    txtStatus.text = msg
                    toast(msg)
                }
            }
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
        txtStatus.text = "正在抓 X 热搜 / 写草稿…"
        try {
            val result = withContext(Dispatchers.IO) { container.repository.runTick() }
            val aiPart = when {
                result.aiError != null -> " · AI失败:${result.aiError.take(40)}"
                result.aiDrafts > 0 -> " · AI ${result.aiDrafts}"
                else -> ""
            }
            val msg =
                "完成：热点 ${result.topics} · 新草稿 ${result.drafts}$aiPart · 发布 ${result.published}"
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
