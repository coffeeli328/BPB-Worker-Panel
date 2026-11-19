package com.gglee.meihuayishu.engine

import java.util.Date
import java.util.UUID

/** 先天八卦数：乾1兑2离3震4巽5坎6艮7坤8 */
enum class Trigram(
    val number: Int,
    val hanzi: String,
    val symbol: String,
    val nature: String,
    val element: WuXing,
    /** 自下而上三爻，true=阳 */
    val lines: BooleanArray,
) {
    QIAN(1, "乾", "☰", "天", WuXing.JIN, booleanArrayOf(true, true, true)),
    DUI(2, "兑", "☱", "泽", WuXing.JIN, booleanArrayOf(true, true, false)),
    LI(3, "离", "☲", "火", WuXing.HUO, booleanArrayOf(true, false, true)),
    ZHEN(4, "震", "☳", "雷", WuXing.MU, booleanArrayOf(true, false, false)),
    XUN(5, "巽", "☴", "风", WuXing.MU, booleanArrayOf(false, true, true)),
    KAN(6, "坎", "☵", "水", WuXing.SHUI, booleanArrayOf(false, true, false)),
    GEN(7, "艮", "☶", "山", WuXing.TU, booleanArrayOf(false, false, true)),
    KUN(8, "坤", "☷", "地", WuXing.TU, booleanArrayOf(false, false, false));

    val displayName: String get() = "$hanzi$symbol"

    fun flipped(lineIndex: Int): Trigram {
        require(lineIndex in 0..2)
        val next = lines.copyOf()
        next[lineIndex] = !next[lineIndex]
        return fromLines(next)
    }

    companion object {
        fun fromNumber(n: Int): Trigram {
            val v = ((n % 8) + 8) % 8
            val num = if (v == 0) 8 else v
            return entries.first { it.number == num }
        }

        fun fromLines(lines: BooleanArray): Trigram =
            entries.first {
                it.lines[0] == lines[0] && it.lines[1] == lines[1] && it.lines[2] == lines[2]
            }
    }
}

enum class WuXing(val hanzi: String) {
    JIN("金"),
    MU("木"),
    SHUI("水"),
    HUO("火"),
    TU("土");

    fun generates(): WuXing = when (this) {
        JIN -> SHUI
        SHUI -> MU
        MU -> HUO
        HUO -> TU
        TU -> JIN
    }

    fun generatedBy(): WuXing = when (this) {
        SHUI -> JIN
        MU -> SHUI
        HUO -> MU
        TU -> HUO
        JIN -> TU
    }

    fun controls(): WuXing = when (this) {
        JIN -> MU
        MU -> TU
        TU -> SHUI
        SHUI -> HUO
        HUO -> JIN
    }

    fun controlledBy(): WuXing = when (this) {
        MU -> JIN
        TU -> MU
        SHUI -> TU
        HUO -> SHUI
        JIN -> HUO
    }

    fun relationTo(other: WuXing): TiYongRelation = when {
        this == other -> TiYongRelation.BI_HE
        generates() == other -> TiYongRelation.TI_SHENG_YONG
        generatedBy() == other -> TiYongRelation.YONG_SHENG_TI
        controls() == other -> TiYongRelation.TI_KE_YONG
        controlledBy() == other -> TiYongRelation.YONG_KE_TI
        else -> TiYongRelation.BI_HE
    }
}

enum class TiYongRelation(val label: String, val hint: String) {
    BI_HE("比和", "体用同行，事体平稳，宜守成"),
    TI_SHENG_YONG("体生用", "我去生彼，多有损耗、付出"),
    YONG_SHENG_TI("用生体", "彼来生我，多得助力、进益"),
    TI_KE_YONG("体克用", "我克彼，事虽可成而费力"),
    YONG_KE_TI("用克体", "彼克我，多阻滞、不利"),
}

enum class CastMethod(val label: String) {
    TIME("时间起卦"),
    NUMBER("数字起卦"),
    MANUAL("手动选卦"),
    RANDOM("随机起卦"),
}

/** 六爻卦，lines[0]=初爻 … lines[5]=上爻，true=阳 */
data class Hexagram(
    val upper: Trigram,
    val lower: Trigram,
) {
    val lines: BooleanArray
        get() = booleanArrayOf(
            lower.lines[0], lower.lines[1], lower.lines[2],
            upper.lines[0], upper.lines[1], upper.lines[2],
        )

    val name: String get() = KingWen.nameOf(upper, lower)
    val title: String get() = "${upper.hanzi}${lower.hanzi} · $name"
    val symbolPair: String get() = "${upper.symbol}${lower.symbol}"

    fun withMovingLine(movingLine: Int): Hexagram {
        require(movingLine in 1..6)
        val idx = movingLine - 1
        val next = lines.copyOf()
        next[idx] = !next[idx]
        return fromLines(next)
    }

    fun mutual(): Hexagram {
        // 互下=2,3,4爻；互上=3,4,5爻（1-based）
        val l = lines
        val lowerMutual = booleanArrayOf(l[1], l[2], l[3])
        val upperMutual = booleanArrayOf(l[2], l[3], l[4])
        return Hexagram(Trigram.fromLines(upperMutual), Trigram.fromLines(lowerMutual))
    }

    companion object {
        fun fromLines(lines: BooleanArray): Hexagram {
            require(lines.size == 6)
            val lower = Trigram.fromLines(booleanArrayOf(lines[0], lines[1], lines[2]))
            val upper = Trigram.fromLines(booleanArrayOf(lines[3], lines[4], lines[5]))
            return Hexagram(upper, lower)
        }

        fun fromNumbers(upperNum: Int, lowerNum: Int): Hexagram =
            Hexagram(Trigram.fromNumber(upperNum), Trigram.fromNumber(lowerNum))
    }
}

data class MeihuaChart(
    val id: UUID = UUID.randomUUID(),
    val createdAt: Date = Date(),
    val queryDate: Date,
    val question: String,
    val method: CastMethod,
    val methodDetail: String,
    val yearBranchNumber: Int? = null,
    val monthNumber: Int? = null,
    val dayNumber: Int? = null,
    val hourBranchNumber: Int? = null,
    val upperNumber: Int,
    val lowerNumber: Int,
    val movingLine: Int,
    val original: Hexagram,
    val mutual: Hexagram,
    val changed: Hexagram,
    val ti: Trigram,
    val yong: Trigram,
    val tiYongRelation: TiYongRelation,
    val ganzhiLine: String,
    val interpretations: List<InterpretationItem>,
) {
    val hasQuestion: Boolean get() = question.isNotBlank()
    val summaryTitle: String
        get() = buildString {
            append(original.name)
            append(" → ")
            append(changed.name)
            append(" · 动")
            append(movingLine)
        }
}

data class InterpretationItem(
    val id: UUID = UUID.randomUUID(),
    val title: String,
    val detail: String,
    val tone: Tone,
)

enum class Tone { POSITIVE, NEUTRAL, CAUTION }

object KingWen {
    private val names = mapOf(
        "乾乾" to "乾为天", "坤坤" to "坤为地", "坎坎" to "坎为水", "离离" to "离为火",
        "震震" to "震为雷", "巽巽" to "巽为风", "艮艮" to "艮为山", "兑兑" to "兑为泽",
        "乾坤" to "天地否", "坤乾" to "地天泰",
        "乾坎" to "天水讼", "坎乾" to "水天需",
        "乾离" to "天火同人", "离乾" to "火天大有",
        "乾震" to "天雷无妄", "震乾" to "雷天大壮",
        "乾巽" to "天风姤", "巽乾" to "风天小畜",
        "乾艮" to "天山遁", "艮乾" to "山天大畜",
        "乾兑" to "天泽履", "兑乾" to "泽天夬",
        "坤坎" to "地水师", "坎坤" to "水地比",
        "坤离" to "地火明夷", "离坤" to "火地晋",
        "坤震" to "地雷复", "震坤" to "雷地豫",
        "坤巽" to "地风升", "巽坤" to "风地观",
        "坤艮" to "地山谦", "艮坤" to "山地剥",
        "坤兑" to "地泽临", "兑坤" to "泽地萃",
        "坎离" to "水火既济", "离坎" to "火水未济",
        "坎震" to "水雷屯", "震坎" to "雷水解",
        "坎巽" to "水风井", "巽坎" to "风水涣",
        "坎艮" to "水山蹇", "艮坎" to "山水蒙",
        "坎兑" to "水泽节", "兑坎" to "泽水困",
        "离震" to "火雷噬嗑", "震离" to "雷火丰",
        "离巽" to "火风鼎", "巽离" to "风火家人",
        "离艮" to "火山旅", "艮离" to "山火贲",
        "离兑" to "火泽睽", "兑离" to "泽火革",
        "震巽" to "雷风恒", "巽震" to "风雷益",
        "震艮" to "雷山小过", "艮震" to "山雷颐",
        "震兑" to "雷泽归妹", "兑震" to "泽雷随",
        "巽艮" to "风山渐", "艮巽" to "山风蛊",
        "巽兑" to "风泽中孚", "兑巽" to "泽风大过",
        "艮兑" to "山泽损", "兑艮" to "泽山咸",
    )

    fun nameOf(upper: Trigram, lower: Trigram): String =
        names["${upper.hanzi}${lower.hanzi}"] ?: "${upper.hanzi}${lower.hanzi}"
}
