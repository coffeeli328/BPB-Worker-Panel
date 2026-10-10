package com.gglee.xhotpost.domain

object DraftGenerator {
    private val nicheLabel = mapOf(
        NicheId.TECH to "科技 / AI",
        NicheId.FINANCE to "财经投资",
        NicheId.LIFESTYLE to "生活方式",
        NicheId.CREATOR to "自媒体变现",
        NicheId.LOCAL to "综合热点",
        NicheId.CUSTOM to "自定义赛道",
    )

    fun buildHook(settings: AppSettings): String {
        val url = settings.affiliateUrl.trim()
        return if (url.isNotEmpty()) {
            settings.ctaTemplate.replace("{link}", url)
        } else {
            "关注我，下一条继续拆这个热点怎么变现。"
        }
    }

    fun templateDraft(topic: HotTopic, settings: AppSettings): String {
        val niche = if (settings.niche == NicheId.CUSTOM && settings.customNicheLabel.isNotBlank()) {
            settings.customNicheLabel
        } else {
            nicheLabel[settings.niche] ?: "热点"
        }
        val hook = buildHook(settings)

        val text = if (topic.language == "en" && settings.language != ContentLanguage.ZH) {
            """
            ${topic.title}

            Why it matters for $niche: ${topic.summary}

            Takeaway: spot the signal early, then ship a clear POV.

            $hook
            """.trimIndent()
        } else {
            """
            【热点】${topic.title}

            一句话：${topic.summary}

            对「$niche」创作者意味着：别只转发标题，给出你的判断与下一步动作。

            $hook
            """.trimIndent()
        }
        return stripExcess(text)
    }

    fun stripExcess(text: String): String =
        text.replace(Regex("[^\\S\\n]+"), " ")
            .replace(Regex("\\n{3,}"), "\n\n")
            .trim()
            .take(280)
}
