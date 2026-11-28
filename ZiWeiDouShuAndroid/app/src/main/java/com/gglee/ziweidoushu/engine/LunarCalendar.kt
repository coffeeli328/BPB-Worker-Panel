package com.gglee.ziweidoushu.engine

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.floor

/**
 * Chinese lunar calendar (1900–2100) using classic bit-packed month tables.
 */
object LunarCalendar {

    data class LunarDate(
        val year: Int,
        val month: Int,
        val day: Int,
        val isLeapMonth: Boolean,
        val yearStemBranch: StemBranch,
    ) {
        val display: String
            get() = buildString {
                append(yearStemBranch.name)
                append("年")
                if (isLeapMonth) append("闰")
                append(monthName(month))
                append("月")
                append(dayName(day))
            }
    }

    data class SolarDate(val year: Int, val month: Int, val day: Int)

    data class Ganzhi(
        val year: StemBranch,
        val month: StemBranch,
        val day: StemBranch,
        val hour: StemBranch,
    )

    fun solarToLunar(year: Int, month: Int, day: Int): LunarDate {
        require(year in 1900..2100) { "仅支持 1900–2100" }
        val offset = daysFrom19000131(year, month, day)
        var remaining = offset
        var lunarYear = 1900
        while (lunarYear < 2101 && remaining >= yearDays(lunarYear)) {
            remaining -= yearDays(lunarYear)
            lunarYear++
        }
        val leap = leapMonth(lunarYear)
        var isLeap = false
        var lunarMonth = 1
        while (lunarMonth <= 12) {
            val days = if (isLeap) leapDays(lunarYear) else monthDays(lunarYear, lunarMonth)
            if (remaining < days) break
            remaining -= days
            if (!isLeap && lunarMonth == leap) {
                isLeap = true
            } else {
                if (isLeap) isLeap = false
                lunarMonth++
            }
        }
        val lunarDay = remaining + 1
        return LunarDate(
            year = lunarYear,
            month = lunarMonth,
            day = lunarDay,
            isLeapMonth = isLeap && lunarMonth == leap,
            yearStemBranch = StemBranch.from(lunarYear - 1984),
        )
    }

    fun lunarToSolar(
        lunarYear: Int,
        lunarMonth: Int,
        lunarDay: Int,
        isLeapMonth: Boolean = false,
    ): SolarDate {
        require(lunarYear in 1900..2100)
        var offset = 0
        for (y in 1900 until lunarYear) offset += yearDays(y)
        val leap = leapMonth(lunarYear)
        for (m in 1 until lunarMonth) {
            offset += monthDays(lunarYear, m)
            if (m == leap) offset += leapDays(lunarYear)
        }
        if (isLeapMonth) {
            require(leap == lunarMonth) { "该年无此闰月" }
            offset += monthDays(lunarYear, lunarMonth)
        }
        offset += lunarDay - 1
        return dateFrom19000131(offset)
    }

    fun fourPillars(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        timeZone: TimeZone = TimeZone.getTimeZone("Asia/Shanghai"),
    ): Ganzhi {
        var y = year
        var m = month
        var d = day
        if (hour >= 23) {
            val cal = Calendar.getInstance(timeZone)
            cal.set(y, m - 1, d, 0, 0, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.add(Calendar.DAY_OF_MONTH, 1)
            y = cal.get(Calendar.YEAR)
            m = cal.get(Calendar.MONTH) + 1
            d = cal.get(Calendar.DAY_OF_MONTH)
        }
        val yearSB = yearStemBranchSolar(y, m, d)
        val monthSB = monthStemBranch(yearSB.stem, m, d)
        val daySB = dayStemBranch(y, m, d)
        val hourSB = hourStemBranch(daySB.stem, hour)
        return Ganzhi(yearSB, monthSB, daySB, hourSB)
    }

    fun hourBranch(hour: Int): EarthlyBranch = EarthlyBranch.hourBranch(hour)

    // --- bit tables: lunarInfo[y-1900] ---
    // bits 0-3: leap month days (29/30), bits 4-15: month lengths, bits 16-19: leap month number

    private val LUNAR_INFO = intArrayOf(
        0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
        0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
        0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
        0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
        0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
        0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5b0, 0x14573, 0x052b0, 0x0a9a8, 0x0e950, 0x06aa0,
        0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
        0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b6a0, 0x195a6,
        0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
        0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x05ac0, 0x0ab60, 0x096d5, 0x092e0,
        0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
        0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
        0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
        0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
        0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0,
        0x14b63, 0x09370, 0x049f8, 0x04970, 0x064b0, 0x168a6, 0x0ea50, 0x06b20, 0x1a6c4, 0x0aae0,
        0x0a2e0, 0x0d2e3, 0x0c960, 0x0d557, 0x0d4a0, 0x0da50, 0x05d55, 0x056a0, 0x0a6d0, 0x055d4,
        0x052d0, 0x0a9b8, 0x0a950, 0x0b4a0, 0x0b6a6, 0x0ad50, 0x055a0, 0x0aba4, 0x0a5b0, 0x052b0,
        0x0b273, 0x06930, 0x07337, 0x06aa0, 0x0ad50, 0x14b55, 0x04b60, 0x0a570, 0x054e4, 0x0d160,
        0x0e968, 0x0d520, 0x0daa0, 0x16aa6, 0x056d0, 0x04ae0, 0x0a9d4, 0x0a2d0, 0x0d150, 0x0f252,
        0x0d520,
    )

    private fun info(year: Int): Int = LUNAR_INFO[year - 1900]

    private fun leapMonth(year: Int): Int = info(year) and 0xf

    private fun leapDays(year: Int): Int {
        if (leapMonth(year) == 0) return 0
        return if ((info(year) and 0x10000) != 0) 30 else 29
    }

    private fun monthDays(year: Int, month: Int): Int =
        if ((info(year) and (0x10000 shr month)) != 0) 30 else 29

    private fun yearDays(year: Int): Int {
        var sum = 348
        var i = 0x8000
        while (i > 0x8) {
            if ((info(year) and i) != 0) sum++
            i = i shr 1
        }
        return sum + leapDays(year)
    }

    private fun daysFrom19000131(year: Int, month: Int, day: Int): Int {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.clear()
        cal.set(1900, Calendar.JANUARY, 31)
        val start = cal.timeInMillis
        cal.set(year, month - 1, day)
        val end = cal.timeInMillis
        return ((end - start) / 86_400_000L).toInt()
    }

    private fun dateFrom19000131(offset: Int): SolarDate {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.clear()
        cal.set(1900, Calendar.JANUARY, 31)
        cal.add(Calendar.DAY_OF_MONTH, offset)
        return SolarDate(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
        )
    }

    private fun yearStemBranchSolar(year: Int, month: Int, day: Int): StemBranch {
        var y = year
        // 立春约 2/4
        if (month < 2 || (month == 2 && day < 4)) y -= 1
        return StemBranch.from(y - 1984)
    }

    private fun monthStemBranch(yearStem: HeavenlyStem, month: Int, day: Int): StemBranch {
        val jieMonth = approximateJieMonth(month, day)
        val startStem = when (yearStem) {
            HeavenlyStem.JIA, HeavenlyStem.JI -> HeavenlyStem.BING.rawValue
            HeavenlyStem.YI, HeavenlyStem.GENG -> HeavenlyStem.WU.rawValue
            HeavenlyStem.BING, HeavenlyStem.XIN -> HeavenlyStem.GENG.rawValue
            HeavenlyStem.DING, HeavenlyStem.REN -> HeavenlyStem.REN.rawValue
            HeavenlyStem.WU, HeavenlyStem.GUI -> HeavenlyStem.JIA.rawValue
        }
        val stem = HeavenlyStem.from(startStem + (jieMonth - 1))
        val branch = EarthlyBranch.from(EarthlyBranch.YIN.rawValue + (jieMonth - 1))
        return StemBranch(stem, branch)
    }

    private fun approximateJieMonth(month: Int, day: Int): Int {
        val boundaries = listOf(
            Pair(2, 4), Pair(3, 6), Pair(4, 5), Pair(5, 6), Pair(6, 6), Pair(7, 7),
            Pair(8, 8), Pair(9, 8), Pair(10, 8), Pair(11, 7), Pair(12, 7), Pair(1, 6),
        )
        val md = month * 100 + day
        if (md < 106) return 12
        for (i in 0 until 11) {
            val a = boundaries[i].first * 100 + boundaries[i].second
            val b = boundaries[i + 1].first * 100 + boundaries[i + 1].second
            if (md >= a && md < b) return i + 1
        }
        if (md >= 1207) return 11
        return 12
    }

    private fun dayStemBranch(year: Int, month: Int, day: Int): StemBranch {
        // julianDay 返回该公历日 0h UT 的儒略日（xxx.5）。
        // 锚定：1900-01-01 = 甲戌日（六十甲子序号 10），JD = 2415020.5。
        // 旧实现误用 2415021.0，导致全日柱整体偏一天。
        val jd = julianDay(year, month, day)
        val baseJd = 2415020.5
        val baseIndex = 10
        val delta = floor(jd - baseJd + 1e-9).toInt()
        val idx = ((baseIndex + delta) % 60 + 60) % 60
        return StemBranch.from(idx)
    }

    private fun hourStemBranch(dayStem: HeavenlyStem, hour: Int): StemBranch {
        val branch = EarthlyBranch.hourBranch(hour)
        val startStem = when (dayStem) {
            HeavenlyStem.JIA, HeavenlyStem.JI -> HeavenlyStem.JIA.rawValue
            HeavenlyStem.YI, HeavenlyStem.GENG -> HeavenlyStem.BING.rawValue
            HeavenlyStem.BING, HeavenlyStem.XIN -> HeavenlyStem.WU.rawValue
            HeavenlyStem.DING, HeavenlyStem.REN -> HeavenlyStem.GENG.rawValue
            HeavenlyStem.WU, HeavenlyStem.GUI -> HeavenlyStem.REN.rawValue
        }
        return StemBranch(HeavenlyStem.from(startStem + branch.rawValue), branch)
    }

    private fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val a = floor(y / 100.0)
        val b = 2 - a + floor(a / 4.0)
        return floor(365.25 * (y + 4716)) + floor(30.6001 * (m + 1)) + day + b - 1524.5
    }

    fun monthName(month: Int): String =
        arrayOf("", "正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")[month.coerceIn(1, 12)]

    fun dayName(day: Int): String {
        return when (day) {
            10 -> "初十"
            20 -> "二十"
            30 -> "三十"
            else -> {
                val prefixes = arrayOf("", "初", "十", "廿", "卅")
                val ones = arrayOf("", "一", "二", "三", "四", "五", "六", "七", "八", "九")
                prefixes[day / 10] + ones[day % 10]
            }
        }
    }
}
