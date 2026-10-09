package com.gglee.qimendunjia.engine

import java.util.Date
import java.util.UUID

enum class Palace(val rawValue: Int) {
    KAN1(1),
    KUN2(2),
    ZHEN3(3),
    XUN4(4),
    ZHONG5(5),
    QIAN6(6),
    DUI7(7),
    GEN8(8),
    LI9(9);

    val hanzi: String
        get() = arrayOf("", "坎", "坤", "震", "巽", "中", "乾", "兑", "艮", "离")[rawValue]

    val direction: String
        get() = when (this) {
            KAN1 -> "北"
            KUN2 -> "西南"
            ZHEN3 -> "东"
            XUN4 -> "东南"
            ZHONG5 -> "中"
            QIAN6 -> "西北"
            DUI7 -> "西"
            GEN8 -> "东北"
            LI9 -> "南"
        }

    fun resolvingZhong(): Palace = if (this == ZHONG5) ZHONG_HOST else this

    companion object {
        val gridOrder: List<List<Palace>> = listOf(
            listOf(XUN4, LI9, KUN2),
            listOf(ZHEN3, ZHONG5, DUI7),
            listOf(GEN8, KAN1, QIAN6),
        )

        val yangFly: List<Palace> =
            listOf(KAN1, KUN2, ZHEN3, XUN4, ZHONG5, QIAN6, DUI7, GEN8, LI9)

        val yinFly: List<Palace> =
            listOf(LI9, GEN8, DUI7, QIAN6, ZHONG5, XUN4, ZHEN3, KUN2, KAN1)

        val clockRing: List<Palace> =
            listOf(KAN1, GEN8, ZHEN3, XUN4, LI9, KUN2, DUI7, QIAN6)

        val ZHONG_HOST: Palace = KUN2

        fun fromRaw(raw: Int): Palace? = entries.find { it.rawValue == raw }
    }
}

enum class NineStar(val rawValue: Int) {
    PENG(1),
    RUI(2),
    CHONG(3),
    FU(4),
    QIN(5),
    XIN(6),
    ZHU(7),
    REN(8),
    YING(9);

    val innatePalace: Palace
        get() = Palace.fromRaw(rawValue)!!

    val hanzi: String
        get() = arrayOf("", "天蓬", "天芮", "天冲", "天辅", "天禽", "天心", "天柱", "天任", "天英")[rawValue]

    val shortName: String
        get() = arrayOf("", "蓬", "芮", "冲", "辅", "禽", "心", "柱", "任", "英")[rawValue]

    val isAuspicious: Boolean
        get() = this == CHONG || this == FU || this == XIN || this == REN

    companion object {
        val clockStars: List<NineStar> =
            listOf(PENG, REN, CHONG, FU, YING, RUI, ZHU, XIN)

        fun from(palace: Palace): NineStar = fromRaw(palace.rawValue)!!

        fun fromRaw(raw: Int): NineStar? = entries.find { it.rawValue == raw }
    }
}

enum class EightGate {
    XIU,
    SI,
    SHANG,
    DU,
    JING,
    KAI,
    JING_SHOCK,
    SHENG;

    val displayName: String
        get() = when (this) {
            XIU -> "休门"
            SI -> "死门"
            SHANG -> "伤门"
            DU -> "杜门"
            JING -> "景门"
            KAI -> "开门"
            JING_SHOCK -> "惊门"
            SHENG -> "生门"
        }

    val isAuspicious: Boolean
        get() = this == XIU || this == SHENG || this == KAI || this == JING

    val innatePalace: Palace
        get() = when (this) {
            XIU -> Palace.KAN1
            SI -> Palace.KUN2
            SHANG -> Palace.ZHEN3
            DU -> Palace.XUN4
            JING -> Palace.LI9
            KAI -> Palace.QIAN6
            JING_SHOCK -> Palace.DUI7
            SHENG -> Palace.GEN8
        }

    companion object {
        val clockGates: List<EightGate> =
            listOf(XIU, SHENG, SHANG, DU, JING, SI, JING_SHOCK, KAI)

        fun innate(at: Palace): EightGate? =
            clockGates.firstOrNull { it.innatePalace == at }
                ?: entries.firstOrNull { it.innatePalace == at }
    }
}

enum class EightDeity {
    ZHI_FU,
    TENG_SHE,
    TAI_YIN,
    LIU_HE,
    BAI_HU,
    XUAN_WU,
    JIU_DI,
    JIU_TIAN;

    fun name(isYangDun: Boolean): String = when (this) {
        ZHI_FU -> "值符"
        TENG_SHE -> "腾蛇"
        TAI_YIN -> "太阴"
        LIU_HE -> "六合"
        BAI_HU -> if (isYangDun) "白虎" else "勾陈"
        XUAN_WU -> if (isYangDun) "玄武" else "朱雀"
        JIU_DI -> "九地"
        JIU_TIAN -> "九天"
    }

    val shortName: String
        get() = arrayOf("符", "蛇", "阴", "合", "虎", "武", "地", "天")[ordinal]
}

enum class HeavenlyStem(val rawValue: Int) {
    JIA(0),
    YI(1),
    BING(2),
    DING(3),
    WU(4),
    JI(5),
    GENG(6),
    XIN(7),
    REN(8),
    GUI(9);

    val hanzi: String
        get() = arrayOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")[rawValue]

    companion object {
        fun from(index: Int): HeavenlyStem {
            val n = ((index % 10) + 10) % 10
            return entries[n]
        }
    }
}

enum class EarthlyBranch(val rawValue: Int) {
    ZI(0),
    CHOU(1),
    YIN(2),
    MAO(3),
    CHEN(4),
    SI(5),
    WU(6),
    WEI(7),
    SHEN(8),
    YOU(9),
    XU(10),
    HAI(11);

    val hanzi: String
        get() = arrayOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")[rawValue]

    companion object {
        fun hourBranch(hour: Int): EarthlyBranch {
            val h = ((hour % 24) + 24) % 24
            val idx = ((h + 1) / 2) % 12
            return entries[idx]
        }

        fun from(index: Int): EarthlyBranch {
            val n = ((index % 12) + 12) % 12
            return entries[n]
        }
    }
}

data class StemBranch(
    val stem: HeavenlyStem,
    val branch: EarthlyBranch,
) {
    val name: String get() = stem.hanzi + branch.hanzi

    val sexagenaryIndex: Int
        get() {
            for (i in 0 until 60) {
                if (HeavenlyStem.from(i) == stem && EarthlyBranch.from(i) == branch) return i
            }
            return 0
        }

    companion object {
        private val STEMS = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
        private val BRANCHES = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")

        fun from(sexagenaryIndex: Int): StemBranch {
            val n = ((sexagenaryIndex % 60) + 60) % 60
            return StemBranch(HeavenlyStem.from(n), EarthlyBranch.from(n))
        }

        fun parse(text: String): StemBranch? {
            if (text.length != 2) return null
            val sIdx = STEMS.indexOf(text.substring(0, 1))
            val bIdx = BRANCHES.indexOf(text.substring(1))
            if (sIdx < 0 || bIdx < 0) return null
            return StemBranch(HeavenlyStem.entries[sIdx], EarthlyBranch.entries[bIdx])
        }
    }
}

enum class JuMethod(val rawValue: String) {
    CHAI_BU("拆补"),
    ZHI_YUN("置闰");

    val detail: String
        get() = when (this) {
            CHAI_BU -> "按交节时刻取本节气三元（默认）"
            ZHI_YUN -> "超神接气；芒种/大雪可闰奇"
        }
}

enum class ZhiYunPhase(val rawValue: String) {
    ZHENG_SHOU("正授"),
    CHAO_SHEN("超神"),
    RUN_QI("闰奇"),
    JIE_QI("接气"),
}

enum class CalendarInputMode(val rawValue: String) {
    SOLAR("阳历"),
    LUNAR("农历"),
}

enum class QimenMethod(val rawValue: String) {
    SHI_JIA("时家奇门"),
}

data class PalaceCell(
    val palace: Palace,
    val earthStem: HeavenlyStem?,
    val heavenStem: HeavenlyStem?,
    val star: NineStar?,
    val gate: EightGate?,
    val deity: EightDeity?,
    val isEmpty: Boolean,
    val isZhiFu: Boolean,
    val isZhiShi: Boolean,
)

data class InterpretationItem(
    val title: String,
    val detail: String,
    val tone: Tone = Tone.NEUTRAL,
    val id: UUID = UUID.randomUUID(),
) {
    enum class Tone {
        NEUTRAL,
        AUSPICIOUS,
        CAUTION,
    }
}

data class ChartRequest(
    var date: Date = Date(),
    var calendarMode: CalendarInputMode = CalendarInputMode.SOLAR,
    var method: QimenMethod = QimenMethod.SHI_JIA,
    var juMethod: JuMethod = JuMethod.CHAI_BU,
    var timeZoneSecondsFromGMT: Int? = 8 * 3600,
    var locationNote: String = "北京",
    var longitude: Double = 116.4074,
    var useTrueSolarTime: Boolean = true,
    var question: String = "",
)

data class JuResolution(
    val isYangDun: Boolean,
    val juNumber: Int,
    val yuanName: String,
    val yuanIndex: Int,
    val solarTermName: String,
    val fuTou: StemBranch,
    val juMethod: JuMethod = JuMethod.CHAI_BU,
    val phase: ZhiYunPhase = ZhiYunPhase.ZHENG_SHOU,
    val isRunQi: Boolean = false,
    val solarTermInstant: Date? = null,
)

data class QimenChart(
    val id: UUID,
    val createdAt: Date,
    val queryDate: Date,
    val trueSolarDate: Date,
    val calendarMode: CalendarInputMode,
    val timeZoneIdentifier: String,
    val method: QimenMethod,
    val locationNote: String,
    val longitude: Double,
    val usedTrueSolarTime: Boolean,
    val longitudeCorrectionMinutes: Double,
    val equationOfTimeMinutes: Double,
    val yearSB: StemBranch,
    val monthSB: StemBranch,
    val daySB: StemBranch,
    val hourSB: StemBranch,
    val isYangDun: Boolean,
    val juNumber: Int,
    val solarTermName: String,
    val yuanName: String,
    val juMethod: JuMethod,
    val juPhase: ZhiYunPhase,
    val isRunQi: Boolean,
    val solarTermInstant: Date,
    val zhiFuStar: NineStar,
    val zhiShiGate: EightGate,
    val zhiFuPalace: Palace,
    val zhiShiPalace: Palace,
    val xunKong: List<EarthlyBranch>,
    val cells: List<PalaceCell>,
    val question: String,
    val questionTopic: QuestionTopic,
    val interpretations: List<InterpretationItem>,
) {
    val juTitle: String
        get() {
            val run = if (isRunQi) "·闰奇" else ""
            return "${if (isYangDun) "阳遁" else "阴遁"}${juNumber}局 · $yuanName$run · ${juMethod.rawValue}"
        }

    val ganzhiLine: String
        get() = "${yearSB.name}年 ${monthSB.name}月 ${daySB.name}日 ${hourSB.name}时"

    val hasQuestion: Boolean
        get() = question.trim().isNotEmpty()

    fun cell(forPalace: Palace): PalaceCell? = cells.firstOrNull { it.palace == forPalace }
}

data class LocationPreset(
    val id: String,
    val name: String,
    val longitude: Double,
) {
    companion object {
        val all: List<LocationPreset> = listOf(
            LocationPreset("beijing", "北京", 116.4074),
            LocationPreset("shanghai", "上海", 121.4737),
            LocationPreset("guangzhou", "广州", 113.2644),
            LocationPreset("chengdu", "成都", 104.0665),
            LocationPreset("xian", "西安", 108.9398),
            LocationPreset("wulumuqi", "乌鲁木齐", 87.6168),
            LocationPreset("haerbin", "哈尔滨", 126.5340),
            LocationPreset("hongkong", "香港", 114.1694),
            LocationPreset("taipei", "台北", 121.5654),
            LocationPreset("meridian120", "东经120°(东八区中央)", 120.0),
        )
    }
}
