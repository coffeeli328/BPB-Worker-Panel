package com.gglee.xhotpost.domain

import kotlin.math.absoluteValue

object DraftGenerator {
    private val nicheLabel = mapOf(
        NicheId.TECH to "科技 / AI",
        NicheId.FINANCE to "财经投资",
        NicheId.LIFESTYLE to "生活方式",
        NicheId.CREATOR to "自媒体变现",
        NicheId.LOCAL to "综合热点",
        NicheId.CUSTOM to "自定义赛道",
    )

    fun nicheName(settings: AppSettings): String {
        return if (settings.niche == NicheId.CUSTOM && settings.customNicheLabel.isNotBlank()) {
            settings.customNicheLabel.trim()
        } else {
            nicheLabel[settings.niche] ?: "热点"
        }
    }

    fun buildHook(settings: AppSettings): String {
        val url = settings.affiliateUrl.trim()
        return if (url.isNotEmpty()) {
            settings.ctaTemplate.replace("{link}", url)
        } else {
            ""
        }
    }

    fun preferEnglish(topic: HotTopic, settings: AppSettings): Boolean {
        return when (settings.language) {
            ContentLanguage.EN -> true
            ContentLanguage.ZH -> false
            ContentLanguage.MIXED -> topic.language == "en"
        }
    }

    /**
     * Local template drafts: multiple angles + writing style so posts feel less copy-paste.
     * Persona is lightly woven in via tone hints, not pasted verbatim.
     */
    fun templateDraft(topic: HotTopic, settings: AppSettings): String {
        val niche = nicheName(settings)
        val hook = buildHook(settings)
        val english = preferEnglish(topic, settings)
        val angle = pickAngle(topic, settings)
        val body = if (english) {
            englishBody(topic, niche, settings.writingStyle, angle, settings.persona)
        } else {
            chineseBody(topic, niche, settings.writingStyle, angle, settings.persona)
        }
        val withHook = if (hook.isNotBlank()) "$body\n\n$hook" else body
        return stripExcess(withHook)
    }

    private fun pickAngle(topic: HotTopic, settings: AppSettings): Int {
        val seed = (topic.id + settings.writingStyle.name + settings.persona.take(24))
            .hashCode()
            .absoluteValue
        return seed % 4
    }

    private fun chineseBody(
        topic: HotTopic,
        niche: String,
        style: WritingStyle,
        angle: Int,
        persona: String,
    ): String {
        val title = topic.title.trim()
        val summary = topic.summary.trim().ifBlank { "这件事正在发酵，值得先看一眼。" }
        val softPersona = personaHintZh(persona)

        return when (style) {
            WritingStyle.OPINION -> when (angle) {
                0 -> "多数人在转「$title」，我更在意的是：${clip(summary, 70)}\n\n对做「$niche」的人，别跟风刷存在感——先给出你的判断，再决定要不要下场。$softPersona"
                1 -> "热点：【$title】\n\n我的判断：${clip(summary, 60)}。跟风转发没用，真正拉开差距的是你敢不敢公开立场。\n\n写给「$niche」创作者：$softPersona"
                2 -> "别只盯标题「$title」。\n\n关键信号：${clip(summary, 70)}\n\n做「$niche」的，现在更该问：这件事改不改变你的内容选题？$softPersona"
                else -> "「$title」看起来热闹，实质是：${clip(summary, 65)}\n\n我的态度：宁可慢半拍，也要讲清楚为什么。$softPersona"
            }
            WritingStyle.HOWTO -> when (angle) {
                0 -> "【可执行】关于「$title」\n\n结论：${clip(summary, 55)}\n\n三步：① 记下你的一句话观点 ② 找一个真实例子 ③ 加一句行动建议。适合「$niche」。$softPersona"
                1 -> "热点到手别空转：「$title」\n\n先提取信息：${clip(summary, 50)}\n\n再产出：一条观点帖 + 一条拆解帖。做「$niche」的人，复用同一素材就够。$softPersona"
                2 -> "把「$title」变成内容资产：\n1) 一句话定义问题\n2) 用「${clip(summary, 40)}」做证据\n3) 给读者一个今天就能做的动作\n\n「$niche」赛道通用。$softPersona"
                else -> "操作清单｜$title\n\n背景：${clip(summary, 50)}\n→ 今天发：你的判断\n→ 明天补：案例/数据\n→ 周末复盘：哪条互动高\n\n$softPersona"
            }
            WritingStyle.STORY -> when (angle) {
                0 -> "早上刷到「$title」，第一反应不是转发，是停了一下。\n\n因为：${clip(summary, 65)}\n\n若你也做「$niche」，这种停顿往往就是选题信号。$softPersona"
                1 -> "朋友问我怎么看「$title」。\n\n我说：${clip(summary, 70)}\n\n真正难的不是追热点，是把热闹翻译成你自己的故事。$softPersona"
                2 -> "想象你的读者刚看到「$title」——他们缺的不是信息，是立场。\n\n补一句：${clip(summary, 60)}\n\n「$niche」内容，信任比速度更值钱。$softPersona"
                else -> "有人把「$title」当热闹，有人当警报。\n\n我站后者：${clip(summary, 65)}\n\n$softPersona"
            }
            WritingStyle.CASUAL -> when (angle) {
                0 -> "刷到「$title」……\n\n${clip(summary, 70)}\n\n做「$niche」的，这波要不要跟？我觉得：先写清楚自己的看法再决定。$softPersona"
                1 -> "短评「$title」：${clip(summary, 80)}\n\n一句话：别当复读机。$softPersona"
                2 -> "「$title」挺吵的。\n我只记一点：${clip(summary, 70)}\n\n你们「$niche」圈怎么看？$softPersona"
                else -> "说人话版「$title」：${clip(summary, 85)}\n\n$softPersona"
            }
            WritingStyle.PRO -> when (angle) {
                0 -> "观察｜$title\n摘要：${clip(summary, 70)}\n对「$niche」的含义：优先验证信息源，再形成可复核的观点。$softPersona"
                1 -> "简报：$title\n要点：${clip(summary, 75)}\n建议：记录假设 → 找反例 → 再公开表态。$softPersona"
                2 -> "事件：$title\n解读：${clip(summary, 70)}\n适用「$niche」：把噪声与可行动信号分开。$softPersona"
                else -> "纪要｜$title\n${clip(summary, 80)}\n结论：信息密度优先于情绪。$softPersona"
            }
        }
    }

    private fun englishBody(
        topic: HotTopic,
        niche: String,
        style: WritingStyle,
        angle: Int,
        persona: String,
    ): String {
        val title = topic.title.trim()
        val summary = topic.summary.trim().ifBlank { "This story is moving fast—worth a closer look." }
        val softPersona = personaHintEn(persona)

        return when (style) {
            WritingStyle.OPINION -> when (angle) {
                0 -> "$title\n\nWhat matters: ${clip(summary, 90)}\n\nFor $niche creators: take a stance, don’t just amplify the headline.$softPersona"
                1 -> "Hot take on “$title”: ${clip(summary, 95)}\n\nSignal > noise. Ship a clear POV.$softPersona"
                else -> "Everyone’s sharing “$title.”\nMy read: ${clip(summary, 90)}\n\nIf you cover $niche, decide what you believe before you post.$softPersona"
            }
            WritingStyle.HOWTO -> when (angle) {
                0 -> "Playbook: $title\n\n1) Extract: ${clip(summary, 70)}\n2) Write your one-line take\n3) Add one action for readers\n\nBuilt for $niche.$softPersona"
                else -> "Turn “$title” into content:\n• Fact: ${clip(summary, 70)}\n• Opinion: yours, not the crowd’s\n• CTA: one next step\n\n$softPersona"
            }
            WritingStyle.STORY ->
                "Saw “$title” and paused.\n\nBecause: ${clip(summary, 95)}\n\nThat pause is often the real $niche story.$softPersona"
            WritingStyle.CASUAL ->
                "Quick note on “$title”: ${clip(summary, 110)}\n\nDon’t just RT—add your take.$softPersona"
            WritingStyle.PRO ->
                "Brief | $title\n${clip(summary, 100)}\nImplication for $niche: separate signal from chatter before publishing.$softPersona"
        }
    }

    private fun personaHintZh(persona: String): String {
        val p = persona.trim()
        if (p.isEmpty()) return ""
        // Never dump the whole system prompt into the tweet; pull a short cue.
        val cue = when {
            p.contains("幽默") || p.contains("玩笑") -> "语气轻松一点也没关系。"
            p.contains("犀利") || p.contains("直言") -> "可以说得更直接。"
            p.contains("温和") || p.contains("真诚") -> "语气保持真诚就好。"
            p.contains("专业") || p.contains("分析") -> "尽量把逻辑写清楚。"
            p.length > 8 -> "按你的人设：${clip(p, 28)}"
            else -> ""
        }
        return if (cue.isEmpty()) "" else "\n$cue"
    }

    private fun personaHintEn(persona: String): String {
        val p = persona.trim()
        if (p.isEmpty()) return ""
        return "\nVoice note: ${clip(p, 40)}"
    }

    private fun clip(text: String, max: Int): String {
        val t = text.replace(Regex("\\s+"), " ").trim()
        if (t.length <= max) return t
        return t.take(max - 1).trimEnd() + "…"
    }

    fun stripExcess(text: String): String =
        text.replace(Regex("[^\\S\\n]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
            .take(280)

    fun aiSystemPrompt(settings: AppSettings): String {
        val niche = nicheName(settings)
        val style = when (settings.writingStyle) {
            WritingStyle.OPINION -> "鲜明观点，敢下判断"
            WritingStyle.HOWTO -> "干货拆解，带可执行步骤"
            WritingStyle.STORY -> "故事/场景开头，再落到洞察"
            WritingStyle.CASUAL -> "口语短句，像朋友聊天"
            WritingStyle.PRO -> "专业克制，少感叹号"
        }
        val lang = when (settings.language) {
            ContentLanguage.ZH -> "中文"
            ContentLanguage.EN -> "English"
            ContentLanguage.MIXED -> "与话题语言一致（中或英）"
        }
        val hook = buildHook(settings)
        val hookRule = if (hook.isNotBlank()) {
            "文末可自然带一句推广（不要生硬硬广）：$hook"
        } else {
            "不要硬广，不要留空链接占位。"
        }
        return """
            你是 X/Twitter 短帖写手。严格遵守：
            - 人设：${settings.persona.trim().ifBlank { "务实、真诚、有洞察" }}
            - 赛道：$niche
            - 风格：$style
            - 语言：$lang
            - 只输出一条帖文正文，不要标题、不要引号包裹、不要解释
            - 最多 280 字（含标点与空格）
            - 禁止：【热点】这类模板开头、复读标题、空洞口号、过多 emoji
            - $hookRule
            - 要有具体判断或信息增量，不要「值得关注」之类废话
        """.trimIndent()
    }

    fun aiUserPrompt(topic: HotTopic): String {
        return """
            热点标题：${topic.title}
            摘要：${topic.summary}
            来源：${topic.source}
            请据此写一条可直接发的短帖。
        """.trimIndent()
    }
}
