package com.gglee.meihuayishu.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Update
import com.gglee.meihuayishu.engine.CastMethod
import com.gglee.meihuayishu.engine.Hexagram
import com.gglee.meihuayishu.engine.InterpretationItem
import com.gglee.meihuayishu.engine.MeihuaChart
import com.gglee.meihuayishu.engine.TiYongRelation
import com.gglee.meihuayishu.engine.Tone
import com.gglee.meihuayishu.engine.Trigram
import kotlinx.coroutines.flow.Flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Date
import java.util.UUID

@Entity(tableName = "history")
data class HistoryEntity(
    @PrimaryKey val id: String,
    val createdAt: Long,
    val queryDate: Long,
    val summary: String,
    val question: String,
    val chartJson: String,
    val aiReadingText: String? = null,
    val aiReadingCacheKey: String? = null,
    val aiReadingUpdatedAt: Long? = null,
)

@Dao
interface HistoryDao {
    @Query("SELECT * FROM history ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<HistoryEntity>>

    @Query("SELECT * FROM history WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): HistoryEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entity: HistoryEntity)

    @Update
    suspend fun update(entity: HistoryEntity)

    @Query("DELETE FROM history WHERE id = :id")
    suspend fun delete(id: String)
}

@Database(entities = [HistoryEntity::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao
}

@Serializable
data class ChartSnapshot(
    val id: String,
    val createdAt: Long,
    val queryDate: Long,
    val question: String,
    val method: String,
    val methodDetail: String,
    val yearBranchNumber: Int? = null,
    val monthNumber: Int? = null,
    val dayNumber: Int? = null,
    val hourBranchNumber: Int? = null,
    val upperNumber: Int,
    val lowerNumber: Int,
    val movingLine: Int,
    val originalUpper: Int,
    val originalLower: Int,
    val mutualUpper: Int,
    val mutualLower: Int,
    val changedUpper: Int,
    val changedLower: Int,
    val ti: Int,
    val yong: Int,
    val tiYongRelation: String,
    val ganzhiLine: String,
    val interpretations: List<InterpretationSnapshot>,
)

@Serializable
data class InterpretationSnapshot(
    val id: String,
    val title: String,
    val detail: String,
    val tone: String,
)

object ChartSnapshotCodec {
    private val json = Json {
        ignoreUnknownKeys = true
        encodeDefaults = true
    }

    fun encode(chart: MeihuaChart): String = json.encodeToString(chart.toSnapshot())

    fun decode(raw: String): MeihuaChart = json.decodeFromString<ChartSnapshot>(raw).toChart()

    private fun MeihuaChart.toSnapshot(): ChartSnapshot = ChartSnapshot(
        id = id.toString(),
        createdAt = createdAt.time,
        queryDate = queryDate.time,
        question = question,
        method = method.name,
        methodDetail = methodDetail,
        yearBranchNumber = yearBranchNumber,
        monthNumber = monthNumber,
        dayNumber = dayNumber,
        hourBranchNumber = hourBranchNumber,
        upperNumber = upperNumber,
        lowerNumber = lowerNumber,
        movingLine = movingLine,
        originalUpper = original.upper.number,
        originalLower = original.lower.number,
        mutualUpper = mutual.upper.number,
        mutualLower = mutual.lower.number,
        changedUpper = changed.upper.number,
        changedLower = changed.lower.number,
        ti = ti.number,
        yong = yong.number,
        tiYongRelation = tiYongRelation.name,
        ganzhiLine = ganzhiLine,
        interpretations = interpretations.map {
            InterpretationSnapshot(it.id.toString(), it.title, it.detail, it.tone.name)
        },
    )

    private fun ChartSnapshot.toChart(): MeihuaChart = MeihuaChart(
        id = UUID.fromString(id),
        createdAt = Date(createdAt),
        queryDate = Date(queryDate),
        question = question,
        method = enumValueOf(method),
        methodDetail = methodDetail,
        yearBranchNumber = yearBranchNumber,
        monthNumber = monthNumber,
        dayNumber = dayNumber,
        hourBranchNumber = hourBranchNumber,
        upperNumber = upperNumber,
        lowerNumber = lowerNumber,
        movingLine = movingLine,
        original = Hexagram(Trigram.fromNumber(originalUpper), Trigram.fromNumber(originalLower)),
        mutual = Hexagram(Trigram.fromNumber(mutualUpper), Trigram.fromNumber(mutualLower)),
        changed = Hexagram(Trigram.fromNumber(changedUpper), Trigram.fromNumber(changedLower)),
        ti = Trigram.fromNumber(ti),
        yong = Trigram.fromNumber(yong),
        tiYongRelation = enumValueOf<TiYongRelation>(tiYongRelation),
        ganzhiLine = ganzhiLine,
        interpretations = interpretations.map {
            InterpretationItem(
                id = UUID.fromString(it.id),
                title = it.title,
                detail = it.detail,
                tone = enumValueOf<Tone>(it.tone),
            )
        },
    )
}

class HistoryRepository(private val dao: HistoryDao) {
    fun observeAll(): Flow<List<HistoryEntity>> = dao.observeAll()

    suspend fun getById(id: String): HistoryEntity? = dao.getById(id)

    suspend fun saveChart(chart: MeihuaChart) {
        val summary = buildString {
            append(chart.summaryTitle)
            if (chart.hasQuestion) {
                append(" · ")
                append(chart.question.take(40))
            }
        }
        dao.insert(
            HistoryEntity(
                id = chart.id.toString(),
                createdAt = chart.createdAt.time,
                queryDate = chart.queryDate.time,
                summary = summary,
                question = chart.question,
                chartJson = ChartSnapshotCodec.encode(chart),
            ),
        )
    }

    suspend fun updateAiReading(id: String, text: String, cacheKey: String) {
        val row = dao.getById(id) ?: return
        dao.update(
            row.copy(
                aiReadingText = text,
                aiReadingCacheKey = cacheKey,
                aiReadingUpdatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun delete(id: String) = dao.delete(id)
}

object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "meihua_history.db",
            ).build().also { instance = it }
        }
}
