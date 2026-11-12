package com.gglee.baziyuce

import com.gglee.baziyuce.engine.BaZiEngine
import com.gglee.baziyuce.engine.CalendarStrength
import com.gglee.baziyuce.engine.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class CalendarStrengthTest {

    @Test
    fun dayStrengthsCoverWholeMonth() {
        val tz = TimeZone.getTimeZone("Asia/Shanghai")
        val cal = Calendar.getInstance(tz)
        cal.clear()
        cal.set(1990, Calendar.MAY, 15, 9, 0, 0)
        val chart = BaZiEngine.chart(cal.time, Gender.MALE, tz)
        val days = CalendarStrength.dayStrengthsForMonth(chart, 2024, 2, tz)
        assertEquals(29, days.size)
        days.forEach {
            assertTrue(it.score in 25..95)
            assertTrue(it.tone.isNotBlank())
        }
    }

    @Test
    fun monthAndYearStrengthsBounded() {
        val tz = TimeZone.getTimeZone("Asia/Shanghai")
        val cal = Calendar.getInstance(tz)
        cal.clear()
        cal.set(1988, Calendar.AUGUST, 8, 8, 0, 0)
        val chart = BaZiEngine.chart(cal.time, Gender.FEMALE, tz)
        val months = CalendarStrength.monthStrengthsForYear(chart, 2025, tz)
        assertEquals(12, months.size)
        val years = CalendarStrength.yearStrengths(chart, 2025, span = 2, timeZone = tz)
        assertEquals(5, years.size)
        assertTrue(years.all { it.score in 25..95 })
    }
}
