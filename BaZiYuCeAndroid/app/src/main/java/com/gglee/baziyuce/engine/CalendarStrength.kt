package com.gglee.baziyuce.engine

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

data class DayStrength(
    val year: Int,
    val month: Int,
    val day: Int,
    val score: Int,
    val tone: String,
    val flowing: StemBranch,
)

data class MonthStrength(
    val year: Int,
    val month: Int,
    val score: Int,
    val tone: String,
    val flowing: StemBranch,
)

data class YearStrength(
    val year: Int,
    val score: Int,
    val tone: String,
    val flowing: StemBranch,
)

/** 日历强弱指数：复用 PredictionEngine 日/月/年评分。 */
object CalendarStrength {

    fun dayStrengthsForMonth(
        chart: BaZiChart,
        year: Int,
        month: Int,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): List<DayStrength> {
        val cal = Calendar.getInstance(timeZone)
        cal.clear()
        cal.timeZone = timeZone
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        cal.set(Calendar.HOUR_OF_DAY, 12)
        val days = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        return (1..days).map { d ->
            cal.set(Calendar.DAY_OF_MONTH, d)
            val f = PredictionEngine.forecast(chart, FortunePeriod.DAY, cal.time, timeZone)
            DayStrength(year, month, d, f.score, f.tone, f.flowingPillar)
        }
    }

    fun monthStrengthsForYear(
        chart: BaZiChart,
        year: Int,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): List<MonthStrength> {
        val cal = Calendar.getInstance(timeZone)
        return (1..12).map { m ->
            cal.clear()
            cal.timeZone = timeZone
            cal.set(year, m - 1, 15, 12, 0, 0)
            val f = PredictionEngine.forecast(chart, FortunePeriod.MONTH, cal.time, timeZone)
            MonthStrength(year, m, f.score, f.tone, f.flowingPillar)
        }
    }

    fun yearStrengths(
        chart: BaZiChart,
        centerYear: Int,
        span: Int = 5,
        timeZone: TimeZone = TimeZone.getDefault(),
    ): List<YearStrength> {
        val cal = Calendar.getInstance(timeZone)
        return ((centerYear - span)..(centerYear + span)).map { y ->
            cal.clear()
            cal.timeZone = timeZone
            // mid-year after 立春 window
            cal.set(y, Calendar.JUNE, 15, 12, 0, 0)
            val f = PredictionEngine.forecast(chart, FortunePeriod.YEAR, cal.time, timeZone)
            YearStrength(y, f.score, f.tone, f.flowingPillar)
        }
    }

    fun dateAtNoon(year: Int, month: Int, day: Int, timeZone: TimeZone): Date {
        val cal = Calendar.getInstance(timeZone)
        cal.clear()
        cal.timeZone = timeZone
        cal.set(year, month - 1, day, 12, 0, 0)
        return cal.time
    }
}
