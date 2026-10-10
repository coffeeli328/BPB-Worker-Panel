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
    val language: ContentLanguage = ContentLanguage.ZH,
    val persona: String =
        "你是一位务实的中文创作者，擅长把热点讲清楚，语气真诚、不夸张，偶尔带一点洞察。",
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
)

data class DashboardStats(
    val pendingReview: Int = 0,
    val published: Int = 0,
    val draftsCreated: Int = 0,
    val topicCount: Int = 0,
)
