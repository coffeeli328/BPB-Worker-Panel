package com.gglee.xhotpost.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.gglee.xhotpost.domain.Draft
import com.gglee.xhotpost.domain.DraftStatus
import com.gglee.xhotpost.domain.HotTopic

@Entity(tableName = "hot_topics")
data class HotTopicEntity(
    @PrimaryKey val id: String,
    val title: String,
    val summary: String,
    val source: String,
    val url: String?,
    val score: Int,
    val language: String,
    val fetchedAt: Long,
)

@Entity(tableName = "drafts")
data class DraftEntity(
    @PrimaryKey val id: String,
    val topicId: String,
    val topicTitle: String,
    val text: String,
    val status: String,
    val monetizationHook: String,
    val createdAt: Long,
    val updatedAt: Long,
    val reviewedAt: Long?,
    val publishedAt: Long?,
    val publishError: String?,
    val externalPostId: String?,
    val demo: Boolean,
)

fun HotTopicEntity.toDomain() = HotTopic(
    id = id,
    title = title,
    summary = summary,
    source = source,
    url = url,
    score = score,
    language = language,
    fetchedAt = fetchedAt,
)

fun HotTopic.toEntity() = HotTopicEntity(
    id = id,
    title = title,
    summary = summary,
    source = source,
    url = url,
    score = score,
    language = language,
    fetchedAt = fetchedAt,
)

fun DraftEntity.toDomain() = Draft(
    id = id,
    topicId = topicId,
    topicTitle = topicTitle,
    text = text,
    status = DraftStatus.valueOf(status),
    monetizationHook = monetizationHook,
    createdAt = createdAt,
    updatedAt = updatedAt,
    reviewedAt = reviewedAt,
    publishedAt = publishedAt,
    publishError = publishError,
    externalPostId = externalPostId,
    demo = demo,
)

fun Draft.toEntity() = DraftEntity(
    id = id,
    topicId = topicId,
    topicTitle = topicTitle,
    text = text,
    status = status.name,
    monetizationHook = monetizationHook,
    createdAt = createdAt,
    updatedAt = updatedAt,
    reviewedAt = reviewedAt,
    publishedAt = publishedAt,
    publishError = publishError,
    externalPostId = externalPostId,
    demo = demo,
)
