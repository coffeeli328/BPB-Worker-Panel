package com.gglee.xhotpost.data

import android.content.Context
import com.gglee.xhotpost.data.local.HotpostDao
import com.gglee.xhotpost.data.local.toDomain
import com.gglee.xhotpost.data.local.toEntity
import com.gglee.xhotpost.domain.AiDraftClient
import com.gglee.xhotpost.domain.AppSettings
import com.gglee.xhotpost.domain.Draft
import com.gglee.xhotpost.domain.DraftGenerator
import com.gglee.xhotpost.domain.DraftStatus
import com.gglee.xhotpost.domain.HotTopic
import com.gglee.xhotpost.domain.TrendCollector
import com.gglee.xhotpost.domain.XPublisher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.util.UUID

data class TickResult(
    val topics: Int,
    val drafts: Int,
    val published: Int,
    val message: String? = null,
)

class HotpostRepository(
    private val context: Context,
    private val dao: HotpostDao,
    private val settingsStore: SettingsStore,
    private val trendCollector: TrendCollector = TrendCollector(),
    private val aiDraftClient: AiDraftClient = AiDraftClient(),
) {
    val settings: Flow<AppSettings> = settingsStore.settings

    val topics: Flow<List<HotTopic>> =
        dao.observeTopics().map { list -> list.map { it.toDomain() } }

    val drafts: Flow<List<Draft>> =
        dao.observeDrafts().map { list -> list.map { it.toDomain() } }

    val stats = combine(
        dao.observePendingCount(),
        dao.observePublishedCount(),
        dao.observeDraftTotal(),
        dao.observeTopicCount(),
    ) { pending, published, total, topics ->
        com.gglee.xhotpost.domain.DashboardStats(
            pendingReview = pending,
            published = published,
            draftsCreated = total,
            topicCount = topics,
        )
    }

    suspend fun updateSettings(transform: (AppSettings) -> AppSettings) {
        settingsStore.update(transform)
    }

    suspend fun runTick(): TickResult {
        val settings = settingsStore.settings.first()
        val topics = trendCollector.collect(settings)
        dao.upsertTopics(topics.map { it.toEntity() })

        var draftsCreated = 0
        if (settings.autoDraft) {
            draftsCreated = generateDrafts(settings, topics, settings.maxDraftsPerTick)
        }

        var published = 0
        if (settings.autoPublishApproved && settings.demoMode) {
            val approved = dao.draftsByStatus(DraftStatus.APPROVED.name)
            for (entity in approved) {
                if (publishDraftInternal(entity.toDomain(), settings, openUi = false)) {
                    published++
                }
            }
        }

        return TickResult(
            topics = topics.size,
            drafts = draftsCreated,
            published = published,
        )
    }

    private suspend fun generateDrafts(
        settings: AppSettings,
        topics: List<HotTopic>,
        limit: Int,
    ): Int {
        var created = 0
        for (topic in topics.take(20)) {
            if (created >= limit) break
            if (dao.activeDraftForTopic(topic.id) != null) continue
            val text = draftTextFor(topic, settings)
            val now = System.currentTimeMillis()
            val draft = Draft(
                id = UUID.randomUUID().toString(),
                topicId = topic.id,
                topicTitle = topic.title,
                text = text,
                status = DraftStatus.PENDING_REVIEW,
                monetizationHook = DraftGenerator.buildHook(settings),
                createdAt = now,
                updatedAt = now,
                demo = settings.demoMode,
            )
            dao.upsertDraft(draft.toEntity())
            created++
        }
        return created
    }

    private suspend fun draftTextFor(topic: HotTopic, settings: AppSettings): String {
        if (settings.aiEnabled && settings.aiApiKey.isNotBlank()) {
            val ai = withContext(Dispatchers.IO) {
                aiDraftClient.generate(topic, settings)
            }
            if (!ai.isNullOrBlank()) return ai
        }
        return DraftGenerator.templateDraft(topic, settings)
    }

    /**
     * Rewrite one pending draft. Pass [forceVariant] to nudge local template angle
     * when AI is off (uses timestamp so the next bank index differs).
     */
    suspend fun regenerateDraft(id: String): String? {
        val entity = dao.draftById(id) ?: return null
        if (entity.status != DraftStatus.PENDING_REVIEW.name) return null
        val settings = settingsStore.settings.first()
        val now = System.currentTimeMillis()
        val live = dao.topicById(entity.topicId)?.toDomain()
        val topic = (live ?: HotTopic(
            id = entity.topicId,
            title = entity.topicTitle,
            summary = "",
            source = "regen",
            url = null,
            score = 50,
            language = if (settings.language == com.gglee.xhotpost.domain.ContentLanguage.EN) {
                "en"
            } else {
                "zh"
            },
            fetchedAt = now,
        )).let { base ->
            // Nudge variant selection for local templates.
            base.copy(id = "${base.id}-$now")
        }
        val text = draftTextFor(topic, settings)
        dao.updateDraft(
            entity.copy(
                text = text,
                monetizationHook = DraftGenerator.buildHook(settings),
                updatedAt = now,
            ),
        )
        return text
    }

    /** Rewrite all pending drafts with current style / AI settings. */
    suspend fun regeneratePendingDrafts(): Int {
        val settings = settingsStore.settings.first()
        val pending = dao.draftsByStatus(DraftStatus.PENDING_REVIEW.name)
        var updated = 0
        val now = System.currentTimeMillis()
        for (entity in pending) {
            val live = dao.topicById(entity.topicId)?.toDomain()
            val topic = live ?: HotTopic(
                id = entity.topicId,
                title = entity.topicTitle,
                summary = entity.topicTitle,
                source = "regen",
                url = null,
                score = 50,
                language = if (settings.language == com.gglee.xhotpost.domain.ContentLanguage.EN) {
                    "en"
                } else {
                    "zh"
                },
                fetchedAt = now,
            )
            val text = draftTextFor(topic, settings)
            dao.updateDraft(
                entity.copy(
                    text = text,
                    monetizationHook = DraftGenerator.buildHook(settings),
                    updatedAt = now,
                ),
            )
            updated++
        }
        return updated
    }

    suspend fun saveDraftText(id: String, text: String) {
        val entity = dao.draftById(id) ?: return
        dao.updateDraft(
            entity.copy(
                text = text.take(280),
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun rejectDraft(id: String) {
        val entity = dao.draftById(id) ?: return
        val now = System.currentTimeMillis()
        dao.updateDraft(
            entity.copy(
                status = DraftStatus.REJECTED.name,
                reviewedAt = now,
                updatedAt = now,
            ),
        )
    }

    suspend fun approveDraft(id: String, text: String): Draft {
        val entity = dao.draftById(id) ?: error("draft missing")
        val settings = settingsStore.settings.first()
        val now = System.currentTimeMillis()
        val approved = entity.copy(
            text = text.take(280),
            status = DraftStatus.APPROVED.name,
            reviewedAt = now,
            updatedAt = now,
        )
        dao.updateDraft(approved)
        val domain = approved.toDomain()
        if (settings.autoPublishApproved) {
            publishDraftInternal(domain, settings, openUi = true)
            return dao.draftById(id)!!.toDomain()
        }
        return domain
    }

    suspend fun publishDraft(id: String): Draft {
        val entity = dao.draftById(id) ?: error("draft missing")
        val settings = settingsStore.settings.first()
        publishDraftInternal(entity.toDomain(), settings, openUi = true)
        return dao.draftById(id)!!.toDomain()
    }

    private suspend fun publishDraftInternal(
        draft: Draft,
        settings: AppSettings,
        openUi: Boolean,
    ): Boolean {
        if (draft.status != DraftStatus.APPROVED && draft.status != DraftStatus.PUBLISHED) {
            return false
        }
        val now = System.currentTimeMillis()
        if (settings.demoMode) {
            dao.updateDraft(
                draft.copy(
                    status = DraftStatus.PUBLISHED,
                    publishedAt = now,
                    updatedAt = now,
                    externalPostId = "demo_${draft.id.take(8)}",
                    demo = true,
                    publishError = null,
                ).toEntity(),
            )
            return true
        }

        if (openUi) {
            val opened = XPublisher.openCompose(context, draft.text)
            if (!opened) {
                dao.updateDraft(
                    draft.copy(
                        status = DraftStatus.FAILED,
                        updatedAt = now,
                        publishError = "未找到 X/Twitter 应用或浏览器",
                    ).toEntity(),
                )
                return false
            }
        }

        dao.updateDraft(
            draft.copy(
                status = DraftStatus.PUBLISHED,
                publishedAt = now,
                updatedAt = now,
                externalPostId = "mobile_${now.toString(36)}",
                demo = false,
                publishError = null,
            ).toEntity(),
        )
        return true
    }
}
