package com.gglee.ziweidoushu.data

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
import com.gglee.ziweidoushu.engine.Brightness
import com.gglee.ziweidoushu.engine.EarthlyBranch
import com.gglee.ziweidoushu.engine.FiveElementBureau
import com.gglee.ziweidoushu.engine.Gender
import com.gglee.ziweidoushu.engine.HeavenlyStem
import com.gglee.ziweidoushu.engine.InterpretationItem
import com.gglee.ziweidoushu.engine.InterpretationTone
import com.gglee.ziweidoushu.engine.PalaceCell
import com.gglee.ziweidoushu.engine.PalaceType
import com.gglee.ziweidoushu.engine.PlacedStar
import com.gglee.ziweidoushu.engine.Star
import com.gglee.ziweidoushu.engine.StemBranch
import com.gglee.ziweidoushu.engine.DecadeLimit
import com.gglee.ziweidoushu.engine.ZiWeiChart
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
    val birthDate: Long,
    val summary: String,
    val name: String,
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
    val birthDate: Long,
    val name: String,
    val gender: String,
    val calendarNote: String,
    val lunarLine: String,
    val yearSB: String,
    val monthSB: String,
    val daySB: String,
    val hourSB: String,
    val bureau: String,
    val mingBranch: Int,
    val shenBranch: Int,
    val ziweiBranch: Int,
    val cells: List<CellSnapshot>,
    val decades: List<DecadeSnapshot>,
    val interpretations: List<InterpretationSnapshot>,
)

@Serializable
data class CellSnapshot(
    val type: String,
    val branch: Int,
    val stem: Int,
    val isMing: Boolean,
    val isShen: Boolean,
    val stars: List<StarSnapshot>,
    val decadeAgeStart: Int,
    val decadeAgeEnd: Int,
)

@Serializable
data class StarSnapshot(val star: String, val brightness: String)

@Serializable
data class DecadeSnapshot(
    val palaceType: String,
    val branch: Int,
    val ageStart: Int,
    val ageEnd: Int,
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

    fun encode(chart: ZiWeiChart): String = json.encodeToString(chart.toSnapshot())

    fun decode(raw: String): ZiWeiChart = json.decodeFromString<ChartSnapshot>(raw).toChart()

    private fun ZiWeiChart.toSnapshot() = ChartSnapshot(
        id = id.toString(),
        createdAt = createdAt.time,
        birthDate = birthDate.time,
        name = name,
        gender = gender.name,
        calendarNote = calendarNote,
        lunarLine = lunarLine,
        yearSB = yearSB.name,
        monthSB = monthSB.name,
        daySB = daySB.name,
        hourSB = hourSB.name,
        bureau = fiveElementBureau.name,
        mingBranch = mingBranch.rawValue,
        shenBranch = shenBranch.rawValue,
        ziweiBranch = ziweiBranch.rawValue,
        cells = cells.map { it.toSnapshot() },
        decades = decades.map {
            DecadeSnapshot(it.palaceType.name, it.branch.rawValue, it.ageStart, it.ageEnd)
        },
        interpretations = interpretations.map {
            InterpretationSnapshot(it.id.toString(), it.title, it.detail, it.tone.name)
        },
    )

    private fun PalaceCell.toSnapshot() = CellSnapshot(
        type = type.name,
        branch = branch.rawValue,
        stem = stem.rawValue,
        isMing = isMing,
        isShen = isShen,
        stars = stars.map { StarSnapshot(it.star.name, it.brightness.name) },
        decadeAgeStart = decadeAgeStart,
        decadeAgeEnd = decadeAgeEnd,
    )

    private fun ChartSnapshot.toChart(): ZiWeiChart = ZiWeiChart(
        id = UUID.fromString(id),
        createdAt = Date(createdAt),
        birthDate = Date(birthDate),
        name = name,
        gender = enumValueOf(gender),
        calendarNote = calendarNote,
        lunarLine = lunarLine,
        yearSB = StemBranch.parse(yearSB) ?: StemBranch.from(0),
        monthSB = StemBranch.parse(monthSB) ?: StemBranch.from(0),
        daySB = StemBranch.parse(daySB) ?: StemBranch.from(0),
        hourSB = StemBranch.parse(hourSB) ?: StemBranch.from(0),
        fiveElementBureau = enumValueOf(bureau),
        mingBranch = EarthlyBranch.from(mingBranch),
        shenBranch = EarthlyBranch.from(shenBranch),
        ziweiBranch = EarthlyBranch.from(ziweiBranch),
        cells = cells.map { it.toCell() },
        decades = decades.map {
            DecadeLimit(enumValueOf(it.palaceType), EarthlyBranch.from(it.branch), it.ageStart, it.ageEnd)
        },
        interpretations = interpretations.map {
            InterpretationItem(UUID.fromString(it.id), it.title, it.detail, enumValueOf(it.tone))
        },
    )

    private fun CellSnapshot.toCell() = PalaceCell(
        type = enumValueOf(type),
        branch = EarthlyBranch.from(branch),
        stem = HeavenlyStem.from(stem),
        isMing = isMing,
        isShen = isShen,
        stars = stars.map {
            PlacedStar(enumValueOf(it.star), enumValueOf(it.brightness))
        },
        decadeAgeStart = decadeAgeStart,
        decadeAgeEnd = decadeAgeEnd,
    )
}

class HistoryRepository(private val dao: HistoryDao) {
    fun observeAll(): Flow<List<HistoryEntity>> = dao.observeAll()

    suspend fun getById(id: String): HistoryEntity? = dao.getById(id)

    suspend fun saveChart(chart: ZiWeiChart) {
        dao.insert(
            HistoryEntity(
                id = chart.id.toString(),
                createdAt = chart.createdAt.time,
                birthDate = chart.birthDate.time,
                summary = chart.title,
                name = chart.name,
                chartJson = ChartSnapshotCodec.encode(chart),
            ),
        )
    }

    suspend fun delete(id: String) = dao.delete(id)

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
}

object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "ziwei_history.db",
            ).build().also { instance = it }
        }
}
