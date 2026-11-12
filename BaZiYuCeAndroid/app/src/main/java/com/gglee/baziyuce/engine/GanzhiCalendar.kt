package com.gglee.baziyuce.engine

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.floor

/**
 * 四柱干支。年柱立春、月柱十二节；日柱儒略日；时柱五鼠遁。
 * 与 iOS BaZiYuCe.GanzhiCalendar 同口径。
 */
object GanzhiCalendar {

    fun stemBranchFourPillars(date: Date, timeZone: TimeZone): FourPillarsResult {
        val cal = Calendar.getInstance(timeZone)
        cal.time = date
        val y = cal.get(Calendar.YEAR)
        val m = cal.get(Calendar.MONTH) + 1
        val d = cal.get(Calendar.DAY_OF_MONTH)
        val hour = cal.get(Calendar.HOUR_OF_DAY)

        val daySB = dayStemBranch(y, m, d, hour, timeZone)
        val hourSB = hourStemBranch(daySB.stem, hour)
        val (yearSB, yearNote) = yearStemBranch(date, timeZone)
        val (monthSB, monthNote) = monthStemBranch(date, yearSB.stem, timeZone)
        return FourPillarsResult(
            year = yearSB,
            month = monthSB,
            day = daySB,
            hour = hourSB,
            note = listOf(yearNote, monthNote).joinToString("；"),
        )
    }

    fun flowingYear(date: Date, timeZone: TimeZone): StemBranch =
        yearStemBranch(date, timeZone).first

    fun flowingMonth(date: Date, timeZone: TimeZone): StemBranch {
        val yearStem = yearStemBranch(date, timeZone).first.stem
        return monthStemBranch(date, yearStem, timeZone).first
    }

    fun flowingDay(date: Date, timeZone: TimeZone): StemBranch {
        val cal = Calendar.getInstance(timeZone)
        cal.time = date
        return dayStemBranch(
            year = cal.get(Calendar.YEAR),
            month = cal.get(Calendar.MONTH) + 1,
            day = cal.get(Calendar.DAY_OF_MONTH),
            hour = cal.get(Calendar.HOUR_OF_DAY),
            timeZone = timeZone,
        )
    }

    fun yearStemBranch(date: Date, timeZone: TimeZone): Pair<StemBranch, String> {
        @Suppress("UNUSED_VARIABLE")
        val unusedTz = timeZone
        val lichun = nearestPastOrEqualTerm(named = "立春", onOrBefore = date)
            ?: SolarTerms.terms(forSolarYear = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply { time = date }.get(Calendar.YEAR))
                .first { it.name == "立春" }

        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.time = lichun.approximateDate
        val yearOfLiChun = cal.get(Calendar.YEAR)
        val ganzhiYear = if (date >= lichun.approximateDate) yearOfLiChun else yearOfLiChun - 1
        // 1984 = 甲子
        val sb = StemBranch.from(sexagenaryIndex = ganzhiYear - 1984)
        return sb to "年柱以立春换年（本次立春参考 ${formatCST(lichun.approximateDate)}）"
    }

    fun monthStemBranch(
        date: Date,
        yearStem: HeavenlyStem,
        timeZone: TimeZone,
    ): Pair<StemBranch, String> {
        @Suppress("UNUSED_VARIABLE")
        val unusedTz = timeZone
        val (jieMonth, jieName, jieDate) = jieMonthInfo(date)
        val startStem = when (yearStem) {
            HeavenlyStem.JIA, HeavenlyStem.JI -> HeavenlyStem.BING.ordinal
            HeavenlyStem.YI, HeavenlyStem.GENG -> HeavenlyStem.WU.ordinal
            HeavenlyStem.BING, HeavenlyStem.XIN -> HeavenlyStem.GENG.ordinal
            HeavenlyStem.DING, HeavenlyStem.REN -> HeavenlyStem.REN.ordinal
            HeavenlyStem.WU, HeavenlyStem.GUI -> HeavenlyStem.JIA.ordinal
        }
        val stem = HeavenlyStem.from(startStem + (jieMonth - 1))
        val branch = EarthlyBranch.from(EarthlyBranch.YIN.ordinal + (jieMonth - 1))
        val sb = StemBranch(stem, branch)
        return sb to "月柱以${jieName}换月（参考 ${formatCST(jieDate)}）"
    }

    /** 返回 1=寅月 … 12=丑月 */
    fun jieMonthInfo(date: Date): Triple<Int, String, Date> {
        val terms = SolarTerms.termsForNearbyYears(around = date)
        val jieNames = listOf(
            "立春", "惊蛰", "清明", "立夏", "芒种", "小暑",
            "立秋", "白露", "寒露", "立冬", "大雪", "小寒",
        )
        val jies = terms.filter { it.name in jieNames }.sortedBy { it.approximateDate }
        val current = jies.lastOrNull { it.approximateDate <= date } ?: jies.firstOrNull()
            ?: return Triple(1, "立春", date)
        val idx = jieNames.indexOf(current.name).coerceAtLeast(0)
        val month = idx + 1
        return Triple(month, current.name, current.approximateDate)
    }

    fun dayStemBranch(year: Int, month: Int, day: Int, hour: Int, timeZone: TimeZone): StemBranch {
        var y = year
        var m = month
        var d = day
        if (hour >= 23) {
            val cal = Calendar.getInstance(timeZone)
            cal.set(Calendar.YEAR, y)
            cal.set(Calendar.MONTH, m - 1)
            cal.set(Calendar.DAY_OF_MONTH, d)
            cal.set(Calendar.HOUR_OF_DAY, 12)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            cal.add(Calendar.DAY_OF_MONTH, 1)
            y = cal.get(Calendar.YEAR)
            m = cal.get(Calendar.MONTH) + 1
            d = cal.get(Calendar.DAY_OF_MONTH)
        }
        val jd = julianDay(y, m, d)
        // 1900-01-01 0h UT 的儒略日为 2415020.5；该日干支为甲戌（index 10）
        val baseJD = 2415020.5
        val baseIndex = 10
        val delta = floor(jd - baseJD).toInt()
        val idx = ((baseIndex + delta) % 60 + 60) % 60
        return StemBranch.from(sexagenaryIndex = idx)
    }

    fun hourStemBranch(dayStem: HeavenlyStem, hour: Int): StemBranch {
        val branch = EarthlyBranch.hourBranch(hour)
        val startStem = when (dayStem) {
            HeavenlyStem.JIA, HeavenlyStem.JI -> HeavenlyStem.JIA.ordinal
            HeavenlyStem.YI, HeavenlyStem.GENG -> HeavenlyStem.BING.ordinal
            HeavenlyStem.BING, HeavenlyStem.XIN -> HeavenlyStem.WU.ordinal
            HeavenlyStem.DING, HeavenlyStem.REN -> HeavenlyStem.GENG.ordinal
            HeavenlyStem.WU, HeavenlyStem.GUI -> HeavenlyStem.REN.ordinal
        }
        return StemBranch(
            stem = HeavenlyStem.from(startStem + branch.ordinal),
            branch = branch,
        )
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

    private fun nearestPastOrEqualTerm(named: String, onOrBefore: Date): SolarTermInfo? {
        val terms = SolarTerms.termsForNearbyYears(around = onOrBefore).filter { it.name == named }
        return terms.lastOrNull { it.approximateDate <= onOrBefore } ?: terms.firstOrNull()
    }

    private fun formatCST(date: Date): String {
        val f = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.CHINA)
        f.timeZone = TimeZone.getTimeZone("GMT+8")
        return f.format(date) + " CST"
    }
}
