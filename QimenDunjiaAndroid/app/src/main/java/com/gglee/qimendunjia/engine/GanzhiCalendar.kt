package com.gglee.qimendunjia.engine

import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.floor

object GanzhiCalendar {

    data class FourPillars(
        val year: StemBranch,
        val month: StemBranch,
        val day: StemBranch,
        val hour: StemBranch,
    )

    fun stemBranchFourPillars(forDate: Date, timeZone: TimeZone): FourPillars {
        val cal = Calendar.getInstance(timeZone)
        cal.time = forDate
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        val daySB = dayStemBranch(year = y, month = m, day = d, hour = hour, timeZone = timeZone)
        val hourSB = hourStemBranch(dayStem = daySB.stem, hour = hour)
        val yearSB = yearStemBranch(solarYear = y, month = m, day = d)
        val monthSB = monthStemBranch(yearStem = yearSB.stem, month = m, day = d)
        return FourPillars(yearSB, monthSB, daySB, hourSB)
    }

    fun yearStemBranch(solarYear: Int, month: Int, day: Int): StemBranch {
        var y = solarYear
        if (month < 2 || (month == 2 && day < 4)) y -= 1
        return StemBranch.from(sexagenaryIndex = y - 1984)
    }

    fun monthStemBranch(yearStem: HeavenlyStem, month: Int, day: Int): StemBranch {
        val jieMonth = approximateJieMonth(month = month, day = day)
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

    fun dayStemBranch(year: Int, month: Int, day: Int, hour: Int, timeZone: TimeZone): StemBranch {
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
        val jd = julianDay(year = y, month = m, day = d)
        val baseJD = 2415021.0
        val baseIndex = 10
        val delta = floor(jd - baseJD).toInt()
        val idx = ((baseIndex + delta) % 60 + 60) % 60
        return StemBranch.from(idx)
    }

    fun hourStemBranch(dayStem: HeavenlyStem, hour: Int): StemBranch {
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

    fun julianDay(year: Int, month: Int, day: Int): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val A = y / 100
        val B = 2 - A + A / 4
        return floor(365.25 * (y + 4716).toDouble()) +
            floor(30.6001 * (m + 1).toDouble()) +
            day + B - 1524.5
    }

    fun lunarDescription(forDate: Date, timeZone: TimeZone): String {
        // Android lacks Calendar.CHINESE in all API levels consistently; simplified placeholder matching iOS structure
        val cal = Calendar.getInstance(timeZone)
        cal.time = forDate
        val stems = listOf("甲", "乙", "丙", "丁", "戊", "己", "庚", "辛", "壬", "癸")
        val branches = listOf("子", "丑", "寅", "卯", "辰", "巳", "午", "未", "申", "酉", "戌", "亥")
        val yearSB = yearStemBranch(
            solarYear = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
        )
        val yIdx = yearSB.sexagenaryIndex
        val yearName = stems[yIdx % 10] + branches[yIdx % 12]
        val months = listOf("正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊")
        val m = cal.get(Calendar.MONTH) + 1
        val monthName = months[(m - 1).coerceIn(0, 11)]
        return "${yearName}年${monthName}月${cal.get(Calendar.DAY_OF_MONTH)}日"
    }
}
