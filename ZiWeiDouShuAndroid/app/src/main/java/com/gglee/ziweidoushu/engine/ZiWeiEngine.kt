package com.gglee.ziweidoushu.engine

import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.ceil

object ZiWeiEngine {

    fun cast(request: CastRequest): ZiWeiChart {
        val tz = TimeZone.getTimeZone("Asia/Shanghai")
        val (solarY, solarM, solarD) = if (request.useLunar) {
            val s = LunarCalendar.lunarToSolar(
                request.year,
                request.month,
                request.day,
                request.lunarLeapMonth,
            )
            Triple(s.year, s.month, s.day)
        } else {
            Triple(request.year, request.month, request.day)
        }

        val lunar = LunarCalendar.solarToLunar(solarY, solarM, solarD)
        val pillars = LunarCalendar.fourPillars(solarY, solarM, solarD, request.hour, tz)
        val hourBranch = LunarCalendar.hourBranch(request.hour)

        val mingBranch = locateMingPalace(lunar.month, hourBranch)
        val shenBranch = locateShenPalace(lunar.month, hourBranch)
        val palaceStems = palaceStems(pillars.year.stem)

        val mingStem = palaceStems[mingBranch.rawValue]
        val bureau = fiveElementBureau(StemBranch(mingStem, mingBranch))

        val ziweiOffsetFromYin = locateZiweiOffset(lunar.day, bureau.number)
        val ziweiBranch = EarthlyBranch.from(EarthlyBranch.YIN.rawValue + ziweiOffsetFromYin)
        val tianfuBranch = EarthlyBranch.from(EarthlyBranch.YIN.rawValue + ((12 - ziweiOffsetFromYin) % 12))

        val starMap = mutableMapOf<EarthlyBranch, MutableList<PlacedStar>>()
        EarthlyBranch.entries.forEach { starMap[it] = mutableListOf() }

        val natalMarks = SiHuaResolver.natalMarkMap(pillars.year.stem)

        fun put(star: Star, branch: EarthlyBranch) {
            starMap[branch]!!.add(
                PlacedStar(
                    star = star,
                    brightness = brightnessOf(star, branch),
                    natalSiHua = natalMarks[star],
                ),
            )
        }

        // 紫微星系（逆）
        put(Star.ZI_WEI, ziweiBranch)
        put(Star.TIAN_JI, step(ziweiBranch, -1))
        put(Star.TAI_YANG, step(ziweiBranch, -3))
        put(Star.WU_QU, step(ziweiBranch, -4))
        put(Star.TIAN_TONG, step(ziweiBranch, -5))
        put(Star.LIAN_ZHEN, step(ziweiBranch, -8))

        // 天府星系（顺）
        put(Star.TIAN_FU, tianfuBranch)
        put(Star.TAI_YIN, step(tianfuBranch, 1))
        put(Star.TAN_LANG, step(tianfuBranch, 2))
        put(Star.JU_MEN, step(tianfuBranch, 3))
        put(Star.TIAN_XIANG, step(tianfuBranch, 4))
        put(Star.TIAN_LIANG, step(tianfuBranch, 5))
        put(Star.QI_SHA, step(tianfuBranch, 6))
        put(Star.PO_JUN, step(tianfuBranch, 10))

        // 辅星
        put(Star.ZUO_FU, step(EarthlyBranch.CHEN, lunar.month - 1))
        put(Star.YOU_BI, step(EarthlyBranch.XU, -(lunar.month - 1)))
        put(Star.WEN_CHANG, step(EarthlyBranch.XU, -hourBranch.rawValue))
        put(Star.WEN_QU, step(EarthlyBranch.CHEN, hourBranch.rawValue))

        val (kui, yue) = kuiYue(pillars.year.stem)
        put(Star.TIAN_KUI, kui)
        put(Star.TIAN_YUE, yue)

        val lu = luCun(pillars.year.stem)
        put(Star.LU_CUN, lu)
        put(Star.QING_YANG, step(lu, 1))
        put(Star.TUO_LUO, step(lu, -1))
        put(Star.TIAN_MA, tianMa(pillars.year.branch))

        val (huo, ling) = huoLing(pillars.year.branch, hourBranch)
        put(Star.HUO_XING, huo)
        put(Star.LING_XING, ling)
        put(Star.DI_KONG, step(EarthlyBranch.HAI, -hourBranch.rawValue))
        put(Star.DI_JIE, step(EarthlyBranch.HAI, hourBranch.rawValue))

        // 十二宫：从命宫逆行
        val palaceOrder = PalaceType.entries
        val branchOfPalace = mutableMapOf<PalaceType, EarthlyBranch>()
        palaceOrder.forEachIndexed { idx, type ->
            branchOfPalace[type] = step(mingBranch, -idx)
        }

        // 阳干年：甲丙戊庚壬（rawValue 偶数）；阳男阴女顺行大限，阴男阳女逆行
        val yinYangYear = pillars.year.stem.rawValue % 2 == 0
        val decadeForward = (request.gender == Gender.MALE && yinYangYear) ||
            (request.gender == Gender.FEMALE && !yinYangYear)

        val startAge = bureau.number
        val decades = mutableListOf<DecadeLimit>()
        repeat(12) { i ->
            val branch = if (decadeForward) step(mingBranch, i) else step(mingBranch, -i)
            val type = branchOfPalace.entries.first { it.value == branch }.key
            val ageStart = startAge + i * 10
            decades += DecadeLimit(type, branch, ageStart, ageStart + 9)
        }
        val decadeByBranch = decades.associateBy { it.branch }

        val cells = palaceOrder.map { type ->
            val branch = branchOfPalace[type]!!
            val dec = decadeByBranch[branch]!!
            PalaceCell(
                type = type,
                branch = branch,
                stem = palaceStems[branch.rawValue],
                isMing = branch == mingBranch,
                isShen = branch == shenBranch,
                stars = starMap[branch]!!.sortedWith(
                    compareBy<PlacedStar> { it.star.category.ordinal }.thenBy { it.star.ordinal },
                ),
                decadeAgeStart = dec.ageStart,
                decadeAgeEnd = dec.ageEnd,
            )
        }

        val birthCal = Calendar.getInstance(tz).apply {
            set(solarY, solarM - 1, solarD, request.hour, request.minute, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val natalSiHua = SiHuaResolver.resolve(pillars.year.stem, cells)

        val chart = ZiWeiChart(
            birthDate = birthCal.time,
            name = request.name.trim(),
            gender = request.gender,
            calendarNote = if (request.useLunar) "农历输入" else "公历输入",
            lunarLine = lunar.display + " " + hourBranch.hanzi + "时",
            yearSB = pillars.year,
            monthSB = pillars.month,
            daySB = pillars.day,
            hourSB = pillars.hour,
            fiveElementBureau = bureau,
            mingBranch = mingBranch,
            shenBranch = shenBranch,
            ziweiBranch = ziweiBranch,
            cells = cells,
            decades = decades,
            natalSiHua = natalSiHua,
            interpretations = emptyList(),
        )
        return chart.copy(interpretations = InterpretationEngine.interpret(chart))
    }

    /** 立春换年后的流年干支（简化：公历年干支，2/4 前用上一年）。 */
    fun annualStemBranch(solarYear: Int, month: Int = 6, day: Int = 15): StemBranch {
        var y = solarYear
        if (month < 2 || (month == 2 && day < 4)) y -= 1
        return StemBranch.from(y - 1984)
    }

    /** 命宫：寅起正月，顺数至生月；再从该宫起子时，逆数至生时 */
    fun locateMingPalace(lunarMonth: Int, hour: EarthlyBranch): EarthlyBranch {
        val monthPalace = step(EarthlyBranch.YIN, lunarMonth - 1)
        return step(monthPalace, -hour.rawValue)
    }

    /** 身宫：寅起正月顺数生月，再顺数生时 */
    fun locateShenPalace(lunarMonth: Int, hour: EarthlyBranch): EarthlyBranch {
        val monthPalace = step(EarthlyBranch.YIN, lunarMonth - 1)
        return step(monthPalace, hour.rawValue)
    }

    /** 五虎遁：起寅宫天干 */
    fun palaceStems(yearStem: HeavenlyStem): Array<HeavenlyStem> {
        val yinStem = when (yearStem) {
            HeavenlyStem.JIA, HeavenlyStem.JI -> HeavenlyStem.BING
            HeavenlyStem.YI, HeavenlyStem.GENG -> HeavenlyStem.WU
            HeavenlyStem.BING, HeavenlyStem.XIN -> HeavenlyStem.GENG
            HeavenlyStem.DING, HeavenlyStem.REN -> HeavenlyStem.REN
            HeavenlyStem.WU, HeavenlyStem.GUI -> HeavenlyStem.JIA
        }
        return Array(12) { i ->
            // 寅=2 为起点
            val offset = (i - EarthlyBranch.YIN.rawValue + 12) % 12
            HeavenlyStem.from(yinStem.rawValue + offset)
        }
    }

    /** 命宫干支纳音 → 五行局 */
    fun fiveElementBureau(ming: StemBranch): FiveElementBureau {
        val nayin = nayinElement(ming)
        return when (nayin) {
            0 -> FiveElementBureau.METAL_4
            1 -> FiveElementBureau.WOOD_3
            2 -> FiveElementBureau.WATER_2
            3 -> FiveElementBureau.FIRE_6
            else -> FiveElementBureau.EARTH_5
        }
    }

    /** 0金 1木 2水 3火 4土 */
    fun nayinElement(sb: StemBranch): Int {
        val i = (0 until 60).firstOrNull {
            it % 10 == sb.stem.rawValue && it % 12 == sb.branch.rawValue
        } ?: 0
        // 纳音三十组：海中金、炉中火、大林木、路旁土、剑锋金…
        val standard = intArrayOf(
            0, 3, 1, 4, 0,
            3, 2, 4, 0, 1,
            2, 4, 3, 1, 2,
            0, 3, 1, 4, 0,
            3, 2, 4, 0, 1,
            2, 4, 3, 1, 2,
        )
        return standard[(i / 2) % 30]
    }

    /**
     * 紫微落宫相对寅的偏移 0..11
     * 口诀：日数÷局数，整除则商数为位；不整除则商+1，再按余数奇偶进退。
     */
    fun locateZiweiOffset(lunarDay: Int, ju: Int): Int {
        var quotient = lunarDay / ju
        val remainder = lunarDay % ju
        val offset = if (remainder == 0) {
            quotient - 1
        } else {
            quotient += 1
            val diff = ju - remainder
            if (diff % 2 == 0) {
                quotient + diff / 2 - 1
            } else {
                quotient - ceil(diff / 2.0).toInt() - 1
            }
        }
        return (offset % 12 + 12) % 12
    }

    private fun step(from: EarthlyBranch, delta: Int): EarthlyBranch =
        EarthlyBranch.from(from.rawValue + delta)

    private fun kuiYue(yearStem: HeavenlyStem): Pair<EarthlyBranch, EarthlyBranch> = when (yearStem) {
        HeavenlyStem.JIA, HeavenlyStem.WU -> EarthlyBranch.CHOU to EarthlyBranch.WEI
        HeavenlyStem.YI, HeavenlyStem.JI -> EarthlyBranch.ZI to EarthlyBranch.SHEN
        HeavenlyStem.BING, HeavenlyStem.DING -> EarthlyBranch.HAI to EarthlyBranch.YOU
        HeavenlyStem.GENG, HeavenlyStem.XIN -> EarthlyBranch.WU to EarthlyBranch.YIN
        HeavenlyStem.REN, HeavenlyStem.GUI -> EarthlyBranch.MAO to EarthlyBranch.SI
    }

    private fun luCun(yearStem: HeavenlyStem): EarthlyBranch = when (yearStem) {
        HeavenlyStem.JIA -> EarthlyBranch.YIN
        HeavenlyStem.YI -> EarthlyBranch.MAO
        HeavenlyStem.BING, HeavenlyStem.WU -> EarthlyBranch.SI
        HeavenlyStem.DING, HeavenlyStem.JI -> EarthlyBranch.WU
        HeavenlyStem.GENG -> EarthlyBranch.SHEN
        HeavenlyStem.XIN -> EarthlyBranch.YOU
        HeavenlyStem.REN -> EarthlyBranch.HAI
        HeavenlyStem.GUI -> EarthlyBranch.ZI
    }

    private fun tianMa(yearBranch: EarthlyBranch): EarthlyBranch = when (yearBranch) {
        EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU -> EarthlyBranch.SHEN
        EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN -> EarthlyBranch.YIN
        EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU -> EarthlyBranch.HAI
        EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI -> EarthlyBranch.SI
    }

    private fun huoLing(yearBranch: EarthlyBranch, hour: EarthlyBranch): Pair<EarthlyBranch, EarthlyBranch> {
        // 火星铃星：按年支三合局，从起始宫顺/逆数时辰
        val (huoStart, lingStart) = when (yearBranch) {
            EarthlyBranch.YIN, EarthlyBranch.WU, EarthlyBranch.XU -> EarthlyBranch.CHOU to EarthlyBranch.MAO
            EarthlyBranch.SHEN, EarthlyBranch.ZI, EarthlyBranch.CHEN -> EarthlyBranch.YIN to EarthlyBranch.XU
            EarthlyBranch.SI, EarthlyBranch.YOU, EarthlyBranch.CHOU -> EarthlyBranch.MAO to EarthlyBranch.XU
            EarthlyBranch.HAI, EarthlyBranch.MAO, EarthlyBranch.WEI -> EarthlyBranch.YOU to EarthlyBranch.XU
        }
        return step(huoStart, hour.rawValue) to step(lingStart, hour.rawValue)
    }

    private fun brightnessOf(star: Star, branch: EarthlyBranch): Brightness {
        if (!star.isMajor) return Brightness.PING
        val b = branch.rawValue
        // Simplified 庙旺表（常用简化）
        val table: Map<Star, IntArray> = mapOf(
            // 子丑寅卯辰巳午未申酉戌亥 → brightness ordinal-ish codes: 0庙1旺2得3利4平5不6陷
            Star.ZI_WEI to intArrayOf(1, 0, 1, 1, 0, 2, 0, 0, 1, 1, 0, 2),
            Star.TIAN_JI to intArrayOf(2, 1, 0, 1, 0, 6, 0, 1, 2, 6, 1, 0),
            Star.TAI_YANG to intArrayOf(6, 6, 1, 0, 1, 1, 0, 2, 2, 4, 6, 6),
            Star.WU_QU to intArrayOf(1, 0, 2, 4, 0, 1, 0, 0, 1, 2, 0, 4),
            Star.TIAN_TONG to intArrayOf(0, 6, 4, 0, 2, 6, 6, 0, 4, 2, 6, 0),
            Star.LIAN_ZHEN to intArrayOf(2, 1, 0, 4, 1, 6, 0, 1, 2, 6, 1, 4),
            Star.TIAN_FU to intArrayOf(0, 0, 0, 2, 0, 1, 1, 0, 0, 2, 0, 1),
            Star.TAI_YIN to intArrayOf(0, 0, 6, 6, 2, 6, 6, 2, 1, 1, 0, 0),
            Star.TAN_LANG to intArrayOf(1, 0, 2, 4, 0, 6, 0, 0, 2, 4, 0, 6),
            Star.JU_MEN to intArrayOf(1, 2, 6, 0, 1, 2, 1, 2, 0, 6, 1, 2),
            Star.TIAN_XIANG to intArrayOf(0, 0, 0, 2, 1, 4, 0, 0, 0, 2, 1, 4),
            Star.TIAN_LIANG to intArrayOf(0, 1, 0, 0, 1, 4, 0, 1, 6, 2, 1, 4),
            Star.QI_SHA to intArrayOf(1, 0, 0, 2, 0, 1, 1, 0, 0, 2, 0, 1),
            Star.PO_JUN to intArrayOf(0, 1, 2, 4, 1, 0, 0, 1, 2, 4, 1, 0),
        )
        val codes = table[star] ?: return Brightness.PING
        return when (codes[b]) {
            0 -> Brightness.MIAO
            1 -> Brightness.WANG
            2 -> Brightness.DE_DI
            3 -> Brightness.LI_DI
            4 -> Brightness.PING
            5 -> Brightness.BU_LI
            else -> Brightness.XIAN
        }
    }
}
