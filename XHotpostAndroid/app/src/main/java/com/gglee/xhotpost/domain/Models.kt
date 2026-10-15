package com.gglee.xhotpost.domain

enum class NicheId {
    TECH,
    FINANCE,
    LIFESTYLE,
    CREATOR,
    LOCAL,
    CUSTOM,
}

enum class ContentLanguage {
    ZH,
    EN,
    MIXED,
}

enum class WritingStyle {
    /** 观点鲜明，像评论区里敢下判断的人 */
    OPINION,
    /** 干货拆解：先结论，再给可执行步骤 */
    HOWTO,
    /** 故事感：场景开头，再落到洞察 */
    STORY,
    /** 更口语、短句、像朋友聊天 */
    CASUAL,
    /** 偏专业、克制，少感叹 */
    PRO,
}

/** X 热搜地区（公开趋势镜像，不用 X API） */
enum class XTrendRegion(
    /** UI 短名 */
    val labelZh: String,
    /** getdaytrends.com path slug；AUTO 为 null */
    val slug: String?,
) {
    AUTO("自动", null),
    WORLDWIDE("全球", "worldwide"),
    UNITED_STATES("美国", "united-states"),
    UNITED_KINGDOM("英国", "united-kingdom"),
    CANADA("加拿大", "canada"),
    AUSTRALIA("澳洲", "australia"),
    JAPAN("日本", "japan"),
    SOUTH_KOREA("韩国", "south-korea"),
    SINGAPORE("新加坡", "singapore"),
    INDIA("印度", "india"),
    INDONESIA("印尼", "indonesia"),
    GERMANY("德国", "germany"),
    BRAZIL("巴西", "brazil"),
    TURKEY("土耳其", "turkey"),
}

enum class DraftStatus {
    PENDING_REVIEW,
    APPROVED,
    REJECTED,
    PUBLISHED,
    FAILED,
}

data class HotTopic(
    val id: String,
    val title: String,
    val summary: String,
    val source: String,
    val url: String?,
    val score: Int,
    val language: String,
    val fetchedAt: Long,
)

data class Draft(
    val id: String,
    val topicId: String,
    val topicTitle: String,
    val text: String,
    val status: DraftStatus,
    val monetizationHook: String,
    val createdAt: Long,
    val updatedAt: Long,
    val reviewedAt: Long? = null,
    val publishedAt: Long? = null,
    val publishError: String? = null,
    val externalPostId: String? = null,
    val demo: Boolean = false,
)

data class AppSettings(
    val displayName: String = "热帖",
    val niche: NicheId = NicheId.TECH,
    val customNicheLabel: String = "",
    /**
     * Extra topics the user always wants tracked (shown in 热点, boosted in ranking).
     * Stored as separate lines in settings UI.
     */
    val watchTopics: List<String> = emptyList(),
    val language: ContentLanguage = ContentLanguage.ZH,
    /** 跟踪哪个地区的 X 热搜 */
    val xTrendRegion: XTrendRegion = XTrendRegion.AUTO,
    val writingStyle: WritingStyle = WritingStyle.CASUAL,
    val persona: String =
        "像真人在 X 上说话：短句、有观点、不鸡汤、不复读热搜。犀利但克制，偶尔一句俏皮。",
    val affiliateUrl: String = "",
    val affiliateLabel: String = "了解更多",
    val ctaTemplate: String = "对这个话题感兴趣的话，可以看看：{link}",
    val autoDraft: Boolean = true,
    val autoPublishApproved: Boolean = true,
    val pollIntervalMinutes: Int = 30,
    val maxDraftsPerTick: Int = 3,
    val demoMode: Boolean = true,
    /** User confirmed / WebView detected X login (no X API). */
    val xLoggedIn: Boolean = false,
    val xUsername: String = "",
    /**
     * Use OpenAI-compatible chat API when enabled + key set.
     * DeepSeek: base `https://api.deepseek.com/v1`, model `deepseek-chat`.
     */
    val aiEnabled: Boolean = false,
    val aiBaseUrl: String = AiDraftClient.DEEPSEEK_BASE,
    val aiApiKey: String = "",
    val aiModel: String = AiDraftClient.DEEPSEEK_MODEL,
)

/** Result of local/AI draft generation for UI feedback. */
data class DraftGenOutcome(
    val text: String,
    val usedAi: Boolean,
    val aiError: String? = null,
)

data class DashboardStats(
    val pendingReview: Int = 0,
    val published: Int = 0,
    val draftsCreated: Int = 0,
    val topicCount: Int = 0,
)
