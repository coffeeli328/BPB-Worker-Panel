package com.gglee.xhotpost.domain

import kotlin.math.absoluteValue

/**
 * Builds short, native-feeling X posts.
 * Never dumps meta summaries ("X 热搜 · 第 N 名") or persona instructions into the tweet.
 */
object DraftGenerator {
    private val nicheLabel = mapOf(
        NicheId.TECH to "科技",
        NicheId.FINANCE to "财经",
        NicheId.LIFESTYLE to "生活",
        NicheId.CREATOR to "创作",
        NicheId.LOCAL to "热点",
        NicheId.CUSTOM to "赛道",
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

    fun templateDraft(topic: HotTopic, settings: AppSettings): String {
        val title = cleanTitle(topic.title)
        val signal = usableSignal(topic.summary)
        val english = preferEnglish(topic, settings)
        val idx = pickIndex(topic, settings, 8)
        val body = if (english) {
            englishPost(title, signal, settings.niche, settings.writingStyle, idx)
        } else {
            chinesePost(title, signal, settings.niche, settings.writingStyle, idx)
        }
        return withOptionalHook(body, settings)
    }

    private fun withOptionalHook(body: String, settings: AppSettings): String {
        val hook = buildHook(settings)
        if (hook.isBlank()) return stripExcess(body)
        // Keep posts scannable: only append CTA if body stays short.
        val merged = if (body.length <= 200) "$body\n\n$hook" else body
        return stripExcess(merged)
    }

    private fun cleanTitle(raw: String): String =
        raw.trim().replace(Regex("\\s+"), " ")

    /** Drop collector meta text; keep only real editorial summaries. */
    fun usableSignal(summary: String?): String? {
        val s = summary?.trim().orEmpty()
        if (s.isBlank()) return null
        if (s.contains("X 热搜") || s.contains("第 ") && s.contains("名")) return null
        if (s.contains("打开可看") || s.contains("适合结合")) return null
        if (s.startsWith("http")) return null
        return s.take(120)
    }

    private fun pickIndex(topic: HotTopic, settings: AppSettings, mod: Int): Int {
        val seed = (topic.id + settings.writingStyle.name + settings.niche.name + settings.persona.take(16))
            .hashCode()
            .absoluteValue
        return seed % mod
    }

    private fun chinesePost(
        title: String,
        signal: String?,
        niche: NicheId,
        style: WritingStyle,
        idx: Int,
    ): String {
        val t = title
        val extra = signal?.let { "（${clip(it, 42)}）" }.orEmpty()

        val bank = when (style) {
            WritingStyle.OPINION -> listOf(
                "热搜又是 $t$extra。\n我的看法：热闹不重要，重要的是你能不能说出一个别人没说过的判断。",
                "$t 上热搜了。\n跟风转发零成本，公开立场才有成本——你站哪边，写清楚。",
                "别被 $t 带节奏。\n先问三个字：所以呢？答不出来，就先别发。",
                "关于 $t：我只信可核对的信息，不信情绪复读。",
                "$t 很吵。\n吵的时候更适合慢半拍：把事实、猜测、观点拆开写。",
                "看见 $t 先别急着表态。\n把「我不知道」写出来，比假装懂更有价值。",
                "$t$extra\n一句话：速度不是优势，清晰才是。",
                "大家都在聊 $t。\n我更想看反例——谁被打脸了，谁提前押对了。",
            )
            WritingStyle.HOWTO -> listOf(
                "怎么跟 $t：\n1) 用一句话定义它\n2) 找一个真实例子\n3) 给读者一个今天能做的动作\n别只发情绪。",
                "$t 可以这样写成帖：\n观点 → 证据 → 行动。三行就够，别写成小作文。",
                "追 $t 的实用做法：先截三张一手信息，再写你的判断，最后删掉一半形容词。",
                "把 $t 拆成两条：一条讲发生了什么，一条讲你怎么看。分开发，互动通常更好。",
                "$t$extra\n今天只做一件事：写清「谁受益 / 谁受损」。",
                "热点清单｜$t\n□ 核实来源  □ 写下判断  □ 加一条可执行建议  □ 再发",
                "别空转 $t：\n用「如果…那么…」写一条假设，邀请同行证伪。",
                "$t 怎么产出：同一素材，早上观点帖，晚上案例帖。",
            )
            WritingStyle.STORY -> listOf(
                "刷到 $t，我停了一下。\n不是因为它火，是因为这类话题最容易让人假装自己懂。",
                "朋友发来 $t 问我怎么看。\n我说：先把情绪放下，再谈信息。",
                "昨晚时间线全是 $t。\n合上手机前我只记一句：信任比速度值钱。",
                "有人把 $t 当热闹，有人当警报。\n我更倾向后者——至少认真核对一遍。",
                "$t$extra\n像在提醒：内容太多时，沉默也是一种判断。",
                "想起第一次追类似热搜的时候，也是这种吵法。\n这次我想慢一点：$t。",
                "时间线被 $t 刷屏。\n我回了句：给我一个可验证的点，再继续聊。",
                "看到 $t 有点熟悉。\n熟悉的套路：先吵，再反转，最后没人复盘。",
            )
            WritingStyle.CASUAL -> listOf(
                "$t 又上热搜了哈哈。\n说真的，你们是真关心，还是手滑转发？",
                "短评 $t：先别站队，先看一手信息。",
                "$t$extra\n我就一句话——别当复读机。",
                "刷到 $t……\n要不要跟？取决于你有没有自己的看法。",
                "$t 挺吵的。\n你们怎么看？我先听听。",
                "关于 $t：我暂缓表态。等更多信息。",
                "$t 来了。\n发之前问自己：删掉这个热搜词，帖还成立吗？",
                "说人话：$t 火≠值得你开口。有增量再说。",
            )
            WritingStyle.PRO -> listOf(
                "观察｜$t\n优先核实来源，再形成可复核观点。情绪不是证据。",
                "简报：$t$extra\n建议：假设 → 反例 → 再公开表态。",
                "事件：$t\n把噪声与可行动信号分开，再决定是否参与讨论。",
                "纪要｜$t\n信息密度优先于情绪；不确定就标注不确定。",
                "$t：关注可验证变量，忽略阵营口号。",
                "笔记｜$t\n记录：已知 / 未知 / 待证。三栏比长文有用。",
                "Risk note｜$t\n传播速度↑时，误读成本↑。",
                "Review：$t$extra\n先证据链，后叙事。",
            )
        }

        val nicheTwist = nicheTwistZh(niche, t, idx)
        val base = bank[idx % bank.size]
        // Niche twist replaces generic bank ~half the time for stronger fit.
        return if (idx % 2 == 0 && nicheTwist != null) nicheTwist else base
    }

    private fun nicheTwistZh(niche: NicheId, title: String, idx: Int): String? {
        val t = title
        return when (niche) {
            NicheId.TECH -> listOf(
                "大家都在聊 $t。\n我更关心：谁已经把它接到真实产品/工作流里了？概念帖太多，落地帖太少。",
                "$t 很火。\n做科技内容别只会喊「颠覆」——写清：换了什么输入，得到什么输出。",
                "关于 $t：技术叙事很会讲故事。\n你要问的是成本、可靠性、谁买单。",
                "$t\n一句话标准：能不能复现？不能复现，就先当营销。",
            )
            NicheId.FINANCE -> listOf(
                "$t 上热搜了。\n这种时候最容易被情绪带着走——仓位比观点诚实。",
                "看到 $t，先分清：这是信息，还是叙事？\n叙事能涨粉，信息才能决策。",
                "$t\n提醒自己：热搜≠催化剂。没有传导路径，就别急着下结论。",
                "关于 $t：把「可能」和「已经」写清楚，少一半焦虑。",
            )
            NicheId.CREATOR -> listOf(
                "$t 可以追，但别当复读机。\n你的增量是什么：角度、案例，还是方法？",
                "热搜 $t 来了。\n创作者真正该问：我的读者为什么要听我讲这个？",
                "$t\n同一热点：一条观点、一条拆解、一条复盘。矩阵比单发强。",
                "别只会蹭 $t。\n把热闹翻译成你自己的选题系统，才算变现能力。",
            )
            NicheId.LIFESTYLE -> listOf(
                "$t 刷屏时，我更想问：它会不会真的改变你明天的选择？\n会，就认真看；不会，就滑走。",
                "关于 $t：生活类热点最怕鸡汤。\n给一个小到能做的动作，比口号有用。",
                "$t\n少一点「你应该」，多一点「我试过」。",
                "看见 $t，先对自己诚实：是好奇，还是焦虑？",
            )
            NicheId.CUSTOM, NicheId.LOCAL -> listOf(
                "$t 在涨。\n我只记可核对的点，其他当背景噪声。",
                "热搜：$t\n发言前先过两关：来源？增量？",
                "$t——热闹过后，留下判断的人更少。我想做后者。",
                "看到 $t。\n先观察，再开口。",
            )
        }.getOrNull(idx % 4)
    }

    private fun englishPost(
        title: String,
        signal: String?,
        niche: NicheId,
        style: WritingStyle,
        idx: Int,
    ): String {
        val t = title
        val extra = signal?.let { " (${clip(it, 48)})" }.orEmpty()
        val bank = when (style) {
            WritingStyle.OPINION -> listOf(
                "$t is trending$extra.\nHot takes are cheap. A clear, falsifiable claim isn’t.",
                "Everyone’s on $t.\nMy rule: if I can’t explain why it matters in one line, I don’t post.",
                "$t\nDon’t amplify the headline. Add a judgment.",
                "Re: $t — speed isn’t an edge. Clarity is.",
            )
            WritingStyle.HOWTO -> listOf(
                "How I’d cover $t:\n1) one-line definition\n2) one real example\n3) one action\nThat’s the post.",
                "$t playbook: fact → take → next step. Cut the adjectives.",
                "Turn $t into two posts: what happened, then what you think. Separate them.",
                "$t$extra\nWrite the “who benefits / who loses” line first.",
            )
            WritingStyle.STORY -> listOf(
                "Saw $t and paused.\nNot because it’s loud — because these topics tempt fake certainty.",
                "A friend pinged me $t.\nI said: drop the vibe, keep the evidence.",
                "Timeline’s all $t.\nI’m taking the slow lane.",
                "$t$extra\nFamiliar pattern: noise first, correction later, no postmortem.",
            )
            WritingStyle.CASUAL -> listOf(
                "$t is loud again.\nAre we informed… or just scrolling with purpose?",
                "Quick on $t: don’t be a RT machine.",
                "$t$extra\nIf the post dies without the trend word, don’t ship it.",
                "Noticing $t. Holding judgment until better sources.",
            )
            WritingStyle.PRO -> listOf(
                "Note | $t\nVerify sources before narrative. Emotion ≠ evidence.",
                "Brief: $t$extra\nHypothesis → counterexample → then publish.",
                "Event: $t\nSeparate signal from chatter before engaging.",
                "Review | $t — prioritize verifiable variables.",
            )
        }
        val nicheBank = when (niche) {
            NicheId.TECH -> listOf(
                "$t is everywhere.\nI care less about the hype and more about: who wired it into a real workflow?",
                "On $t: skip “disruption.” Write the input→output.",
            )
            NicheId.FINANCE -> listOf(
                "$t is trending.\nPosition sizing is more honest than hot takes.",
                "Re: $t — narrative ≠ catalyst. Map the transmission path.",
            )
            NicheId.CREATOR -> listOf(
                "$t can be content — if you add angle, case, or method. Else it’s noise.",
                "Creators: why should your audience hear $t from you?",
            )
            else -> emptyList()
        }
        return if (idx % 2 == 0 && nicheBank.isNotEmpty()) {
            nicheBank[idx % nicheBank.size]
        } else {
            bank[idx % bank.size]
        }
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
            WritingStyle.OPINION -> "鲜明观点，敢下判断，但要具体"
            WritingStyle.HOWTO -> "短干货：2～3 步可执行，不要长教程"
            WritingStyle.STORY -> "一两句场景，再落到洞察"
            WritingStyle.CASUAL -> "口语、短句，像真人在 X 上讲话"
            WritingStyle.PRO -> "克制、信息密度高，少感叹号"
        }
        val lang = when (settings.language) {
            ContentLanguage.ZH -> "中文"
            ContentLanguage.EN -> "English"
            ContentLanguage.MIXED -> "与话题一致（中或英）"
        }
        val hook = buildHook(settings)
        val hookRule = if (hook.isNotBlank()) {
            "若正文已较短，文末可自然带一句：$hook"
        } else {
            "不要硬广，不要留链接占位符。"
        }
        return """
你在写 X（Twitter）帖文，不是写作文，也不是新闻稿。

人设（只影响语气，不要把人设原文写进帖子）：${settings.persona.trim().ifBlank { "务实、真诚、有洞察" }}
赛道：$niche
风格：$style
语言：$lang

硬性规则：
1. 只输出一条可直接发送的正文；不要标题、不要引号包裹、不要解释、不要 hashtag 堆砌
2. 建议 80～180 字，最多 280 字
3. 必须有信息增量或具体判断；禁止「值得关注」「大家怎么看」「冲」「必看」
4. 禁止模板腔：【热点】、一句话：、对创作者意味着、按你的人设
5. 话题名/热搜词自然出现 1 次即可，不要复读
6. 少用 emoji（0～1 个）；不要编号小作文除非风格是干货
7. $hookRule

好例子：
「大家都在聊 #AI。我更关心谁已经把它接到付费流程里了——概念帖太多，落地帖太少。」
坏例子：
「【热点】#AI 一句话：值得关注。对科技创作者意味着：给出判断。」
        """.trimIndent()
    }

    fun aiUserPrompt(topic: HotTopic): String {
        val signal = usableSignal(topic.summary)
        return buildString {
            appendLine("热搜/话题：${cleanTitle(topic.title)}")
            if (signal != null) appendLine("已知背景：$signal")
            appendLine("来源：${topic.source}")
            append("请写一条可直接发的短帖（像真人，不像模板）。")
        }
    }

    /** Caption for resharing an X video / status link (link appended separately). */
    fun linkShareCaption(parsed: ParsedXLink, settings: AppSettings): String {
        val who = parsed.username?.let { "@$it" } ?: "这条"
        val niche = nicheName(settings)
        val english = settings.language == ContentLanguage.EN
        val idx = (
            (parsed.statusId ?: parsed.canonicalUrl) + settings.writingStyle.name
            ).hashCode().absoluteValue % 5

        val body = if (english) {
            when (settings.writingStyle) {
                WritingStyle.CASUAL -> listOf(
                    "Caught this from $who — worth a watch if you care about $niche.",
                    "Saving this clip from $who. Short, clear, actually useful.",
                    "Not doomscrolling — this one from $who earned the pause.",
                    "Quick share: $who nailed a point creators in $niche keep missing.",
                    "If you’re in $niche, watch this before the timeline moves on.",
                )
                WritingStyle.OPINION -> listOf(
                    "Most clips are noise. This one from $who isn’t — it makes a real claim.",
                    "My take after $who’s video: stop copying formats, ship a clearer POV.",
                    "Credit to $who — the cut is simple, the idea isn’t.",
                    "Watch $who’s clip, then ask: what’s the one sentence you’d add?",
                    "For $niche: this is the kind of signal that beats another hot take.",
                )
                WritingStyle.HOWTO -> listOf(
                    "Steal this structure from $who: hook → one proof → one action.",
                    "Creator note: pause $who’s video and list 3 beats you can reuse.",
                    "Workflow: watch once, write your angle, then post with the link.",
                    "Use $who’s clip as a prompt: what’s YOUR version of this point?",
                    "For $niche drafts: attach the link, lead with your judgment first.",
                )
                WritingStyle.STORY -> listOf(
                    "Stopped mid-scroll on $who’s video. That’s rare.",
                    "Friend sent me $who’s clip — stayed for the ending.",
                    "Timeline was loud; $who’s video was the quiet useful one.",
                    "Bookmarking $who for later. Future-me will thank present-me.",
                    "Saw $who’s clip and thought of our last $niche debate.",
                )
                WritingStyle.PRO -> listOf(
                    "Signal | clip via $who — relevant to $niche.",
                    "Reference: $who. Review before amplifying.",
                    "Media note: $who — compact delivery, check claims.",
                    "Share for $niche desk: primary source embedded below.",
                    "Brief: $who video — extract the falsifiable claim first.",
                )
            }[idx]
        } else {
            when (settings.writingStyle) {
                WritingStyle.CASUAL -> listOf(
                    "刚刷到 $who 这个视频，停下来看完了。做「$niche」的可以看看。",
                    "分享一条：$who 讲得比一堆文字清楚。",
                    "不是广告，是真觉得 $who 这条值得存一下。",
                    "时间线太吵，这条 $who 的视频算有用信号。",
                    "$who 这条短视频，比十条复读热搜有用。",
                )
                WritingStyle.OPINION -> listOf(
                    "$who 这条视频有个真实判断，不是情绪复读。做「$niche」的值得对照一下自己的立场。",
                    "看完 $who：热闹不重要，重要的是你能不能补一句别人没说过的。",
                    "多数二创在抄形式，$who 这条至少把观点说清楚了。",
                    "转发 $who 之前先问：我的增量是什么？没有增量就别发。",
                    "关于「$niche」：$who 这条比空喊口号更接近可执行。",
                )
                WritingStyle.HOWTO -> listOf(
                    "可复用结构（来自 $who）：开头钩子 → 一个证据 → 一个动作。你也可以照着写。",
                    "创作者作业：看完 $who，记下 3 个可拆镜头/论点，改成自己的帖。",
                    "用法：先写你的判断，再贴链接。别只扔链接。",
                    "把 $who 当素材库：同一视频可拆观点帖 + 拆解帖。",
                    "「$niche」练习：用一句话概括 $who 的核心，再决定转不转。",
                )
                WritingStyle.STORY -> listOf(
                    "刷到 $who 的视频，本来想滑走，结果看到一半不舍得关。",
                    "朋友丢来 $who 这条，我说：这比今日热搜列表有用。",
                    "晚上时间线全是热闹，唯独 $who 这条让我记了笔记。",
                    "想起自己上次也想讲清楚同一件事——$who 讲得更干脆。",
                    "收藏了 $who。过两天选题荒，大概会回来翻。",
                )
                WritingStyle.PRO -> listOf(
                    "参考｜$who 视频，与「$niche」相关。转发前请自核信息。",
                    "材料：$who。建议先提取可验证主张，再公开表态。",
                    "简报：来源 $who。适合作为讨论底本，而非结论。",
                    "分享供「$niche」对照：原文链接见下。",
                    "笔记｜$who — 信息密度尚可，注意区分事实与观点。",
                )
            }[idx]
        }
        return stripExcess(body)
    }

    fun linkSharePost(parsed: ParsedXLink, caption: String, settings: AppSettings): String {
        val captionClean = caption.trim().ifBlank { linkShareCaption(parsed, settings) }
        val hook = buildHook(settings)
        val withLink = if (captionClean.contains(parsed.canonicalUrl)) {
            captionClean
        } else {
            "$captionClean\n\n${parsed.canonicalUrl}"
        }
        val merged = if (hook.isNotBlank() && withLink.length + hook.length + 2 <= 280) {
            "$withLink\n$hook"
        } else {
            withLink
        }
        return stripExcess(merged)
    }

    /** Caption only — video file is attached separately, so do not append the status URL. */
    fun videoShareCaptionText(parsed: ParsedXLink, caption: String, settings: AppSettings): String {
        val captionClean = caption.trim().ifBlank { linkShareCaption(parsed, settings) }
            .replace(parsed.canonicalUrl, "")
            .replace(Regex("https?://(?:x|twitter)\\.com\\S+"), "")
            .trim()
        val hook = buildHook(settings)
        val merged = if (hook.isNotBlank() && captionClean.length + hook.length + 2 <= 280) {
            "$captionClean\n$hook"
        } else {
            captionClean
        }
        return stripExcess(merged)
    }

    fun aiLinkShareSystemPrompt(settings: AppSettings): String {
        val niche = nicheName(settings)
        val lang = when (settings.language) {
            ContentLanguage.ZH -> "中文"
            ContentLanguage.EN -> "English"
            ContentLanguage.MIXED -> "中文（可夹少量英文）"
        }
        return """
你在帮创作者写一条「转发 X 视频/帖子」的推荐短帖。
人设（勿写入正文）：${settings.persona}
赛道：$niche
语言：$lang
规则：
1. 只输出正文，不要标题/引号/解释
2. 80～160 字为佳；不要复读链接
3. 不要写「值得一看」「冲」等空话；要有一句具体理由
4. 不要在正文里放 URL（链接会另附）
5. 少 emoji
        """.trimIndent()
    }

    fun aiLinkShareUserPrompt(parsed: ParsedXLink): String {
        return buildString {
            appendLine("原帖链接：${parsed.canonicalUrl}")
            if (!parsed.username.isNullOrBlank()) appendLine("作者：@${parsed.username}")
            if (!parsed.statusId.isNullOrBlank()) appendLine("帖子 ID：${parsed.statusId}")
            append("请写一条推荐转发短帖（不含 URL）。")
        }
    }
}
