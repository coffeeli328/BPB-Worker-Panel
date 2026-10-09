package com.gglee.qimendunjia.engine

import java.util.Calendar
import java.util.Date
import java.util.TimeZone

data class TrueSolarAdjustment(
    val date: Date,
    val longitudeMinutes: Double,
    val eotMinutes: Double,
    val totalMinutes: Double,
)

object TrueSolarTime {

    fun standardMeridianDegrees(timeZone: TimeZone, at: Date): Double =
        timeZone.getOffset(at.time).toDouble() / 3600.0 * 15.0

    fun longitudeCorrectionMinutes(longitude: Double, timeZone: TimeZone, at: Date): Double {
        val meridian = standardMeridianDegrees(timeZone, at)
        return (longitude - meridian) * 4.0
    }

    fun adjustedDate(
        civil: Date,
        timeZone: TimeZone,
        longitudeEastDegrees: Double,
        applyEquationOfTime: Boolean = true,
    ): TrueSolarAdjustment {
        val lonMin = longitudeCorrectionMinutes(longitude = longitudeEastDegrees, timeZone = timeZone, at = civil)
        var eot = 0.0
        if (applyEquationOfTime) {
            val jd = AstronomyCore.julianDay(civil)
            val jde = jd + AstronomyCore.deltaTSeconds(yearUT(civil)) / 86400.0
            eot = AstronomyCore.equationOfTimeMinutes(jde)
        }
        val total = lonMin + eot
        val adjusted = Date(civil.time + (total * 60.0).toLong())
        return TrueSolarAdjustment(adjusted, lonMin, eot, total)
    }

    private fun yearUT(of: Date): Double {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.time = of
        return cal.get(Calendar.YEAR).toDouble()
    }
}
