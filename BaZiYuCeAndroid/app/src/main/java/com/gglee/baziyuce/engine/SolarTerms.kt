package com.gglee.baziyuce.engine

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

data class SolarTermInfo(
    val name: String,
    val index: Int,
    val approximateDate: Date,
    val longitudeDegrees: Double,
)

object SolarTerms {

    val names: List<String> = listOf(
        "小寒", "大寒", "立春", "雨水", "惊蛰", "春分",
        "清明", "谷雨", "立夏", "小满", "芒种", "夏至",
        "小暑", "大暑", "立秋", "处暑", "白露", "秋分",
        "寒露", "霜降", "立冬", "小雪", "大雪", "冬至",
    )

    val longitudes: List<Double> = listOf(
        285.0, 300.0, 315.0, 330.0, 345.0, 0.0,
        15.0, 30.0, 45.0, 60.0, 75.0, 90.0,
        105.0, 120.0, 135.0, 150.0, 165.0, 180.0,
        195.0, 210.0, 225.0, 240.0, 255.0, 270.0,
    )

    @Suppress("UNUSED_PARAMETER")
    fun currentTerm(forDate: Date, timeZone: TimeZone): SolarTermInfo {
        // timeZone kept for iOS API parity; solar terms are absolute instants
        val terms = termsForNearbyYears(around = forDate)
        val past = terms.filter { it.approximateDate <= forDate }
        return past.lastOrNull() ?: terms[0]
    }

    fun termsForNearbyYears(around: Date): List<SolarTermInfo> {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.time = around
        val year = cal.get(Calendar.YEAR)
        val result = mutableListOf<SolarTermInfo>()
        for (y in (year - 1)..(year + 1)) {
            result.addAll(terms(forSolarYear = y))
        }
        return result.sortedBy { it.approximateDate }
    }

    fun terms(forSolarYear: Int): List<SolarTermInfo> =
        names.indices.map { idx ->
            val lon = longitudes[idx]
            val date = instant(year = forSolarYear, termIndex = idx)
            SolarTermInfo(name = names[idx], index = idx + 1, approximateDate = date, longitudeDegrees = lon)
        }

    fun instant(year: Int, termIndex: Int): Date {
        val lon = longitudes[termIndex]
        val guessDay = 5.0 + termIndex * 15.2184
        val guessJD = AstronomyCore.julianDay(year = year, month = 1, day = 1, hourUT = 0.0) + guessDay
        val jde = AstronomyCore.solveSolarLongitude(targetDegrees = lon, jdeGuess = guessJD)
        val dt = AstronomyCore.deltaTSeconds(year = year.toDouble())
        val jdUT = jde - dt / 86400.0
        return AstronomyCore.date(fromJulianDayUT = jdUT)
    }
}
