package com.gglee.baziyuce.engine

import java.util.Calendar
import java.util.Date
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.tan

/**
 * Meeus 低精度太阳黄经 + ΔT（1900–2100）。
 * 与 iOS BaZiYuCe / QimenDunjiaAndroid 同口径。
 */
object AstronomyCore {

    const val J2000 = 2451545.0

    fun deltaTSeconds(year: Double): Double {
        val y = year
        val t = y - 2000.0
        if (y < 1900 || y > 2100) {
            val u = (y - 1820.0) / 100.0
            return -20.0 + 32.0 * u * u
        }
        if (y < 1920) {
            return -2.79 + 1.494119 * t - 0.0598939 * t * t + 0.0061966 * t * t * t - 0.000197 * t * t * t * t
        }
        if (y < 1941) {
            val u = y - 1920
            return 21.20 + 0.84493 * u - 0.07613 * u.pow(2) + 0.0020936 * u.pow(3)
        }
        if (y < 1961) {
            val u = y - 1950
            return 29.07 + 0.407 * u - u.pow(2) / 233.0 + u.pow(3) / 2547.0
        }
        if (y < 1986) {
            val u = y - 1975
            return 45.45 + 1.067 * u - u.pow(2) / 260.0 - u.pow(3) / 718.0
        }
        if (y < 2005) {
            val u = y - 2000
            return 63.86 + 0.3345 * u - 0.060374 * u * u + 0.0017275 * u * u * u +
                0.000651814 * u.pow(4) + 0.00002373599 * u.pow(5)
        }
        if (y < 2050) {
            return 62.92 + 0.32217 * t + 0.005589 * t * t
        }
        return -20 + 32 * ((y - 1820) / 100).pow(2) - 0.5628 * (2100 - y)
    }

    fun sunApparentLongitudeDegrees(jde: Double): Double {
        val T = (jde - J2000) / 36525.0
        val L0 = rev360(280.46646 + 36000.76983 * T + 0.0003032 * T * T)
        val M = rev360(357.52911 + 35999.05029 * T - 0.0001537 * T * T)
        val Mr = M * Math.PI / 180.0
        val C = (1.914602 - 0.004817 * T - 0.000014 * T * T) * sin(Mr) +
            (0.019993 - 0.000101 * T) * sin(2 * Mr) +
            0.000289 * sin(3 * Mr)
        val trueLong = L0 + C
        val omega = 125.04 - 1934.136 * T
        return rev360(trueLong - 0.00569 - 0.00478 * sin(omega * Math.PI / 180.0))
    }

    fun equationOfTimeMinutes(jde: Double): Double {
        val T = (jde - J2000) / 36525.0
        val L0 = rev360(280.46646 + 36000.76983 * T + 0.0003032 * T * T)
        val M = 357.52911 + 35999.05029 * T - 0.0001537 * T * T
        val e = 0.016708634 - T * (0.000042037 + 0.0000001267 * T)
        val eps0 = 23.0 + (26.0 + (21.448 - T * (46.815 + T * (0.00059 - T * 0.001813))) / 60.0) / 60.0
        val omega = 125.04 - 1934.136 * T
        val eps = eps0 + 0.00256 * cos(omega * Math.PI / 180.0)
        val y = tan(eps / 2 * Math.PI / 180.0).pow(2)
        val Mr = M * Math.PI / 180.0
        val L0r = L0 * Math.PI / 180.0
        val eotRad = y * sin(2 * L0r) -
            2 * e * sin(Mr) +
            4 * e * y * sin(Mr) * cos(2 * L0r) -
            0.5 * y * y * sin(4 * L0r) -
            1.25 * e * e * sin(2 * Mr)
        return 4.0 * eotRad * 180 / Math.PI
    }

    fun solveSolarLongitude(targetDegrees: Double, jdeGuess: Double): Double {
        var jde = jdeGuess
        val target = rev360(targetDegrees)
        repeat(16) {
            val lon = sunApparentLongitudeDegrees(jde)
            var diff = lon - target
            while (diff > 180) diff -= 360
            while (diff < -180) diff += 360
            val h = 0.02
            var d1 = sunApparentLongitudeDegrees(jde + h) - sunApparentLongitudeDegrees(jde - h)
            while (d1 > 180) d1 -= 360
            while (d1 < -180) d1 += 360
            val rate = d1 / (2 * h)
            if (kotlin.math.abs(rate) <= 1e-12) return@repeat
            val corr = diff / rate
            jde -= corr
            if (kotlin.math.abs(corr) < 1e-8) return@repeat
        }
        return jde
    }

    fun julianDay(year: Int, month: Int, day: Int, hourUT: Double): Double {
        var y = year
        var m = month
        if (m <= 2) {
            y -= 1
            m += 12
        }
        val A = y / 100
        val B = 2 - A + A / 4
        val dayFrac = day + hourUT / 24.0
        return floor(365.25 * (y + 4716).toDouble()) +
            floor(30.6001 * (m + 1).toDouble()) +
            dayFrac + B - 1524.5
    }

    fun julianDay(from: Date): Double {
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.time = from
        val hour = cal.get(Calendar.HOUR_OF_DAY) +
            cal.get(Calendar.MINUTE) / 60.0 +
            cal.get(Calendar.SECOND) / 3600.0
        return julianDay(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            hourUT = hour,
        )
    }

    fun date(fromJulianDayUT: Double): Date {
        val Z = floor(fromJulianDayUT + 0.5).toInt()
        val F = fromJulianDayUT + 0.5 - Z
        val A: Int = if (Z < 2299161) {
            Z
        } else {
            val alpha = floor((Z - 1867216.25) / 36524.25).toInt()
            Z + 1 + alpha - alpha / 4
        }
        val B = A + 1524
        val C = floor((B - 122.1) / 365.25).toInt()
        val D = floor(365.25 * C.toDouble()).toInt()
        val E = floor((B - D).toDouble() / 30.6001).toInt()
        val day = (B - D - floor(30.6001 * E.toDouble())).toDouble() + F
        val month = if (E < 14) E - 1 else E - 13
        val year = if (month > 2) C - 4716 else C - 4715
        val dayI = floor(day).toInt()
        val frac = day - dayI
        val hours = frac * 24
        val h = floor(hours).toInt()
        val minutes = (hours - h) * 60
        val mi = floor(minutes).toInt()
        val sec = (minutes - mi) * 60
        val cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month - 1)
        cal.set(Calendar.DAY_OF_MONTH, dayI)
        cal.set(Calendar.HOUR_OF_DAY, h)
        cal.set(Calendar.MINUTE, mi)
        cal.set(Calendar.SECOND, floor(sec).toInt())
        cal.set(Calendar.MILLISECOND, ((sec - floor(sec)) * 1000).toInt())
        return cal.time
    }

    fun rev360(x: Double): Double {
        val r = x % 360
        return if (r >= 0) r else r + 360
    }
}
