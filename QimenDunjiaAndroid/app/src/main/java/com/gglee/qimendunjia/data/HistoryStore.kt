package com.gglee.qimendunjia.data

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
import com.gglee.qimendunjia.engine.CalendarInputMode
import com.gglee.qimendunjia.engine.EarthlyBranch
import com.gglee.qimendunjia.engine.EightDeity
import com.gglee.qimendunjia.engine.EightGate
import com.gglee.qimendunjia.engine.HeavenlyStem
import com.gglee.qimendunjia.engine.InterpretationItem
import com.gglee.qimendunjia.engine.JuMethod
import com.gglee.qimendunjia.engine.NineStar
import com.gglee.qimendunjia.engine.Palace
import com.gglee.qimendunjia.engine.PalaceCell
import com.gglee.qimendunjia.engine.QimenChart
import com.gglee.qimendunjia.engine.QimenMethod
import com.gglee.qimendunjia.engine.QuestionTopic
import com.gglee.qimendunjia.engine.StemBranch
import com.gglee.qimendunjia.engine.ZhiYunPhase
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
    val trueSolarDate: Long,
    val calendarMode: String,
    val timeZoneIdentifier: String,
    val method: String,
    val locationNote: String,
    val longitude: Double,
    val usedTrueSolarTime: Boolean,
    val longitudeCorrectionMinutes: Double,
    val equationOfTimeMinutes: Double,
    val yearSB: String,
    val monthSB: String,
    val daySB: String,
    val hourSB: String,
    val isYangDun: Boolean,
    val juNumber: Int,
    val solarTermName: String,
    val yuanName: String,
    val juMethod: String,
    val juPhase: String,
    val isRunQi: Boolean,
    val solarTermInstant: Long,
    val zhiFuStarRaw: Int,
    val zhiShiGate: String,
    val zhiFuPalaceRaw: Int,
    val zhiShiPalaceRaw: Int,
    val xunKong: List<Int>,
    val cells: List<CellSnapshot>,
    val question: String,
    val questionTopic: String,
    val interpretations: List<InterpretationSnapshot>,
)

@Serializable
data class CellSnapshot(
    val palaceRaw: Int,
    val earthStemRaw: Int?,
    val heavenStemRaw: Int?,
    val starRaw: Int?,
    val gate: String?,
    val deity: String?,
    val isEmpty: Boolean,
    val isZhiFu: Boolean,
    val isZhiShi: Boolean,
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

    fun encode(chart: QimenChart): String = json.encodeToString(chart.toSnapshot())

    fun decode(raw: String): QimenChart = json.decodeFromString<ChartSnapshot>(raw).toChart()

    fun QimenChart.toSnapshot(): ChartSnapshot = ChartSnapshot(
        id = id.toString(),
        createdAt = createdAt.time,
        queryDate = queryDate.time,
        trueSolarDate = trueSolarDate.time,
        calendarMode = calendarMode.name,
        timeZoneIdentifier = timeZoneIdentifier,
        method = method.name,
        locationNote = locationNote,
        longitude = longitude,
        usedTrueSolarTime = usedTrueSolarTime,
        longitudeCorrectionMinutes = longitudeCorrectionMinutes,
        equationOfTimeMinutes = equationOfTimeMinutes,
        yearSB = yearSB.name,
        monthSB = monthSB.name,
        daySB = daySB.name,
        hourSB = hourSB.name,
        isYangDun = isYangDun,
        juNumber = juNumber,
        solarTermName = solarTermName,
        yuanName = yuanName,
        juMethod = juMethod.name,
        juPhase = juPhase.name,
        isRunQi = isRunQi,
        solarTermInstant = solarTermInstant.time,
        zhiFuStarRaw = zhiFuStar.rawValue,
        zhiShiGate = zhiShiGate.name,
        zhiFuPalaceRaw = zhiFuPalace.rawValue,
        zhiShiPalaceRaw = zhiShiPalace.rawValue,
        xunKong = xunKong.map { it.rawValue },
        cells = cells.map { it.toSnapshot() },
        question = question,
        questionTopic = questionTopic.name,
        interpretations = interpretations.map { it.toSnapshot() },
    )

    private fun PalaceCell.toSnapshot(): CellSnapshot = CellSnapshot(
        palaceRaw = palace.rawValue,
        earthStemRaw = earthStem?.rawValue,
        heavenStemRaw = heavenStem?.rawValue,
        starRaw = star?.rawValue,
        gate = gate?.name,
        deity = deity?.name,
        isEmpty = isEmpty,
        isZhiFu = isZhiFu,
        isZhiShi = isZhiShi,
    )

    private fun InterpretationItem.toSnapshot(): InterpretationSnapshot = InterpretationSnapshot(
        id = id.toString(),
        title = title,
        detail = detail,
        tone = tone.name,
    )

    private fun ChartSnapshot.toChart(): QimenChart {
        val zhiFuPalace = Palace.fromRaw(zhiFuPalaceRaw) ?: Palace.KUN2
        val zhiShiPalace = Palace.fromRaw(zhiShiPalaceRaw) ?: Palace.KUN2
        val zhiFuStar = NineStar.fromRaw(zhiFuStarRaw) ?: NineStar.PENG
        val zhiShiGate = enumValueOf<EightGate>(zhiShiGate)
        return QimenChart(
            id = UUID.fromString(id),
            createdAt = Date(createdAt),
            queryDate = Date(queryDate),
            trueSolarDate = Date(trueSolarDate),
            calendarMode = enumValueOf(calendarMode),
            timeZoneIdentifier = timeZoneIdentifier,
            method = enumValueOf(method),
            locationNote = locationNote,
            longitude = longitude,
            usedTrueSolarTime = usedTrueSolarTime,
            longitudeCorrectionMinutes = longitudeCorrectionMinutes,
            equationOfTimeMinutes = equationOfTimeMinutes,
            yearSB = StemBranch.parse(yearSB) ?: StemBranch.from(0),
            monthSB = StemBranch.parse(monthSB) ?: StemBranch.from(0),
            daySB = StemBranch.parse(daySB) ?: StemBranch.from(0),
            hourSB = StemBranch.parse(hourSB) ?: StemBranch.from(0),
            isYangDun = isYangDun,
            juNumber = juNumber,
            solarTermName = solarTermName,
            yuanName = yuanName,
            juMethod = enumValueOf(juMethod),
            juPhase = enumValueOf(juPhase),
            isRunQi = isRunQi,
            solarTermInstant = Date(solarTermInstant),
            zhiFuStar = zhiFuStar,
            zhiShiGate = zhiShiGate,
            zhiFuPalace = zhiFuPalace,
            zhiShiPalace = zhiShiPalace,
            xunKong = xunKong.map { EarthlyBranch.from(it) },
            cells = cells.map { it.toCell() },
            question = question,
            questionTopic = enumValueOf(questionTopic),
            interpretations = interpretations.map { it.toItem() },
        )
    }

    private fun CellSnapshot.toCell(): PalaceCell {
        val palace = Palace.fromRaw(palaceRaw) ?: Palace.KAN1
        return PalaceCell(
            palace = palace,
            earthStem = earthStemRaw?.let { HeavenlyStem.from(it) },
            heavenStem = heavenStemRaw?.let { HeavenlyStem.from(it) },
            star = starRaw?.let { NineStar.fromRaw(it) },
            gate = gate?.let { enumValueOf<EightGate>(it) },
            deity = deity?.let { enumValueOf<EightDeity>(it) },
            isEmpty = isEmpty,
            isZhiFu = isZhiFu,
            isZhiShi = isZhiShi,
        )
    }

    private fun InterpretationSnapshot.toItem(): InterpretationItem = InterpretationItem(
        id = UUID.fromString(id),
        title = title,
        detail = detail,
        tone = enumValueOf(tone),
    )
}

class HistoryRepository(private val dao: HistoryDao) {
    fun observeAll(): Flow<List<HistoryEntity>> = dao.observeAll()

    suspend fun getById(id: String): HistoryEntity? = dao.getById(id)

    suspend fun saveChart(chart: QimenChart) {
        val summary = buildString {
            append(chart.juTitle)
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
}

object DatabaseProvider {
    @Volatile
    private var instance: AppDatabase? = null

    fun get(context: Context): AppDatabase =
        instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                "qimen_history.db",
            ).build().also { instance = it }
        }
}
