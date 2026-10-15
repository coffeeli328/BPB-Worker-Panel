package com.gglee.xhotpost.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface HotpostDao {
    @Query("SELECT * FROM hot_topics ORDER BY score DESC, fetchedAt DESC LIMIT 80")
    fun observeTopics(): Flow<List<HotTopicEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertTopics(items: List<HotTopicEntity>)

    @Query("SELECT * FROM drafts ORDER BY updatedAt DESC")
    fun observeDrafts(): Flow<List<DraftEntity>>

    @Query("SELECT * FROM drafts WHERE status = :status ORDER BY updatedAt DESC")
    suspend fun draftsByStatus(status: String): List<DraftEntity>

    @Query("SELECT * FROM drafts WHERE id = :id LIMIT 1")
    suspend fun draftById(id: String): DraftEntity?

    @Query("SELECT * FROM hot_topics WHERE id = :id LIMIT 1")
    suspend fun topicById(id: String): HotTopicEntity?

    @Query(
        """
        SELECT * FROM drafts
        WHERE topicId = :topicId
          AND status IN ('PENDING_REVIEW', 'APPROVED', 'PUBLISHED')
        LIMIT 1
        """,
    )
    suspend fun activeDraftForTopic(topicId: String): DraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertDraft(entity: DraftEntity)

    @Update
    suspend fun updateDraft(entity: DraftEntity)

    @Query("SELECT COUNT(*) FROM drafts WHERE status = 'PENDING_REVIEW'")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM drafts WHERE status = 'PUBLISHED'")
    fun observePublishedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM drafts")
    fun observeDraftTotal(): Flow<Int>

    @Query("SELECT COUNT(*) FROM hot_topics")
    fun observeTopicCount(): Flow<Int>
}
