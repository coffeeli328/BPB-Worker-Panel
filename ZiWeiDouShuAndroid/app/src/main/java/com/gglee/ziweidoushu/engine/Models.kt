package com.gglee.ziweidoushu.engine

import java.util.Date
import java.util.UUID

enum class HeavenlyStem(val rawValue: Int) {
    JIA(0), YI(1), BING(2), DING(3), WU(4),
    JI(5), GENG(6), XIN(7), REN(8), GUI(9);

    val hanzi: String
        get() = "甲乙丙丁戊己庚辛壬癸"[rawValue].toString()

    companion object {
        fun from(index: Int): HeavenlyStem = entries[(index % 10 + 10) % 10]
    }
}

enum class EarthlyBranch(val rawValue: Int) {
    ZI(0), CHOU(1), YIN(2), MAO(3), CHEN(4), SI(5),
    WU(6), WEI(7), SHEN(8), YOU(9), XU(10), HAI(11);

    val hanzi: String
        get() = "子丑寅卯辰巳午未申酉戌亥"[rawValue].toString()

    companion object {
        fun from(index: Int): EarthlyBranch = entries[(index % 12 + 12) % 12]

        fun hourBranch(hour: Int): EarthlyBranch = when (hour) {
            23, 0 -> ZI
            in 1..2 -> CHOU
            in 3..4 -> YIN
            in 5..6 -> MAO
            in 7..8 -> CHEN
            in 9..10 -> SI
            in 11..12 -> WU
            in 13..14 -> WEI
            in 15..16 -> SHEN
            in 17..18 -> YOU
            in 19..20 -> XU
            in 21..22 -> HAI
            else -> ZI
        }
    }
}

data class StemBranch(val stem: HeavenlyStem, val branch: EarthlyBranch) {
    val name: String get() = stem.hanzi + branch.hanzi

    companion object {
        fun from(sexagenaryIndex: Int): StemBranch {
            val i = (sexagenaryIndex % 60 + 60) % 60
            return StemBranch(HeavenlyStem.from(i), EarthlyBranch.from(i))
        }

        fun parse(text: String): StemBranch? {
            if (text.length < 2) return null
            val s = HeavenlyStem.entries.find { it.hanzi == text.substring(0, 1) } ?: return null
            val b = EarthlyBranch.entries.find { it.hanzi == text.substring(1, 2) } ?: return null
            return StemBranch(s, b)
        }
    }
}

enum class Gender {
    MALE, FEMALE;

    val displayName: String
        get() = when (this) {
            MALE -> "男"
            FEMALE -> "女"
        }
}

enum class PalaceType(val order: Int) {
    MING(0),
    XIONG_DI(1),
    FU_QI(2),
    ZI_NV(3),
    CAI_BO(4),
    JI_E(5),
    QIAN_YI(6),
    JIAO_YOU(7),
    GUAN_LU(8),
    TIAN_ZHAI(9),
    FU_DE(10),
    FU_MU(11);

    val hanzi: String
        get() = when (this) {
            MING -> "命宫"
            XIONG_DI -> "兄弟"
            FU_QI -> "夫妻"
            ZI_NV -> "子女"
            CAI_BO -> "财帛"
            JI_E -> "疾厄"
            QIAN_YI -> "迁移"
            JIAO_YOU -> "交友"
            GUAN_LU -> "官禄"
            TIAN_ZHAI -> "田宅"
            FU_DE -> "福德"
            FU_MU -> "父母"
        }

    val shortName: String
        get() = when (this) {
            MING -> "命"
            XIONG_DI -> "兄"
            FU_QI -> "夫"
            ZI_NV -> "子"
            CAI_BO -> "财"
            JI_E -> "疾"
            QIAN_YI -> "迁"
            JIAO_YOU -> "友"
            GUAN_LU -> "官"
            TIAN_ZHAI -> "田"
            FU_DE -> "福"
            FU_MU -> "父"
        }
}

enum class FiveElementBureau(val number: Int) {
    WATER_2(2),
    WOOD_3(3),
    METAL_4(4),
    EARTH_5(5),
    FIRE_6(6);

    val displayName: String
        get() = when (this) {
            WATER_2 -> "水二局"
            WOOD_3 -> "木三局"
            METAL_4 -> "金四局"
            EARTH_5 -> "土五局"
            FIRE_6 -> "火六局"
        }
}

enum class StarCategory {
    MAJOR,
    AUXILIARY,
    MISC;

    val displayName: String
        get() = when (this) {
            MAJOR -> "主星"
            AUXILIARY -> "辅星"
            MISC -> "杂曜"
        }
}

enum class Brightness {
    MIAO, WANG, DE_DI, LI_DI, PING, BU_LI, XIAN;

    val hanzi: String
        get() = when (this) {
            MIAO -> "庙"
            WANG -> "旺"
            DE_DI -> "得"
            LI_DI -> "利"
            PING -> "平"
            BU_LI -> "不"
            XIAN -> "陷"
        }
}

enum class Star(
    val hanzi: String,
    val category: StarCategory,
) {
    ZI_WEI("紫微", StarCategory.MAJOR),
    TIAN_JI("天机", StarCategory.MAJOR),
    TAI_YANG("太阳", StarCategory.MAJOR),
    WU_QU("武曲", StarCategory.MAJOR),
    TIAN_TONG("天同", StarCategory.MAJOR),
    LIAN_ZHEN("廉贞", StarCategory.MAJOR),
    TIAN_FU("天府", StarCategory.MAJOR),
    TAI_YIN("太阴", StarCategory.MAJOR),
    TAN_LANG("贪狼", StarCategory.MAJOR),
    JU_MEN("巨门", StarCategory.MAJOR),
    TIAN_XIANG("天相", StarCategory.MAJOR),
    TIAN_LIANG("天梁", StarCategory.MAJOR),
    QI_SHA("七杀", StarCategory.MAJOR),
    PO_JUN("破军", StarCategory.MAJOR),

    ZUO_FU("左辅", StarCategory.AUXILIARY),
    YOU_BI("右弼", StarCategory.AUXILIARY),
    WEN_CHANG("文昌", StarCategory.AUXILIARY),
    WEN_QU("文曲", StarCategory.AUXILIARY),
    TIAN_KUI("天魁", StarCategory.AUXILIARY),
    TIAN_YUE("天钺", StarCategory.AUXILIARY),
    LU_CUN("禄存", StarCategory.AUXILIARY),
    TIAN_MA("天马", StarCategory.AUXILIARY),

    QING_YANG("擎羊", StarCategory.MISC),
    TUO_LUO("陀罗", StarCategory.MISC),
    HUO_XING("火星", StarCategory.MISC),
    LING_XING("铃星", StarCategory.MISC),
    DI_KONG("地空", StarCategory.MISC),
    DI_JIE("地劫", StarCategory.MISC);

    val isMajor: Boolean get() = category == StarCategory.MAJOR
}

data class PlacedStar(
    val star: Star,
    val brightness: Brightness = Brightness.PING,
)

data class PalaceCell(
    val type: PalaceType,
    val branch: EarthlyBranch,
    val stem: HeavenlyStem,
    val isMing: Boolean,
    val isShen: Boolean,
    val stars: List<PlacedStar>,
    val decadeAgeStart: Int,
    val decadeAgeEnd: Int,
) {
    val majorStars: List<PlacedStar> get() = stars.filter { it.star.isMajor }
    val otherStars: List<PlacedStar> get() = stars.filterNot { it.star.isMajor }
}

data class DecadeLimit(
    val palaceType: PalaceType,
    val branch: EarthlyBranch,
    val ageStart: Int,
    val ageEnd: Int,
)

enum class InterpretationTone {
    POSITIVE, NEUTRAL, CAUTION
}

data class InterpretationItem(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val detail: String,
    val tone: InterpretationTone,
)

data class ZiWeiChart(
    val id: UUID = UUID.randomUUID(),
    val createdAt: Date = Date(),
    val birthDate: Date,
    val name: String,
    val gender: Gender,
    val calendarNote: String,
    val lunarLine: String,
    val yearSB: StemBranch,
    val monthSB: StemBranch,
    val daySB: StemBranch,
    val hourSB: StemBranch,
    val fiveElementBureau: FiveElementBureau,
    val mingBranch: EarthlyBranch,
    val shenBranch: EarthlyBranch,
    val ziweiBranch: EarthlyBranch,
    val cells: List<PalaceCell>,
    val decades: List<DecadeLimit>,
    val interpretations: List<InterpretationItem>,
) {
    val title: String
        get() = buildString {
            if (name.isNotBlank()) {
                append(name)
                append(" · ")
            }
            append(fiveElementBureau.displayName)
            append(" · ")
            append(gender.displayName)
            append("命")
        }

    val ganzhiLine: String
        get() = "${yearSB.name}年 ${monthSB.name}月 ${daySB.name}日 ${hourSB.name}时"

    fun cell(type: PalaceType): PalaceCell = cells.first { it.type == type }

    fun cellByBranch(branch: EarthlyBranch): PalaceCell = cells.first { it.branch == branch }
}

data class CastRequest(
    var name: String = "",
    var gender: Gender = Gender.MALE,
    var year: Int = 1990,
    var month: Int = 1,
    var day: Int = 1,
    var hour: Int = 12,
    var minute: Int = 0,
    var useLunar: Boolean = false,
    var lunarLeapMonth: Boolean = false,
)
