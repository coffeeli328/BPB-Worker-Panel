package com.gglee.baziyuce

import com.gglee.baziyuce.engine.BaZiEngine
import com.gglee.baziyuce.engine.GanzhiCalendar
import com.gglee.baziyuce.engine.Gender
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class BirthInputValidationTest {

    @Test
    fun rejectsOutOfRangeYear() {
        assertNotNull(BaZiSessionViewModel.validateBirthFields(1899, 5, 15, 10))
        assertNotNull(BaZiSessionViewModel.validateBirthFields(2101, 5, 15, 10))
    }

    @Test
    fun rejectsInvalidDayForMonth() {
        assertNotNull(BaZiSessionViewModel.validateBirthFields(2023, 2, 29, 10)) // non-leap
        assertNull(BaZiSessionViewModel.validateBirthFields(2024, 2, 29, 10)) // leap
    }

    @Test
    fun composeDateMatchesEngineDayPillar() {
        val tz = TimeZone.getTimeZone("Asia/Shanghai")
        val date = BaZiSessionViewModel.composeBirthDate(2000, 1, 1, 12, tz)
        assertNotNull(date)
        val chart = BaZiEngine.chart(birthDate = date!!, gender = Gender.MALE, timeZone = tz)
        val expected = GanzhiCalendar.dayStemBranch(2000, 1, 1, 12, tz)
        assertEquals(expected.name, chart.day.name)
    }

    @Test
    fun ziHour23StillRollsDayViaComposedDate() {
        val tz = TimeZone.getTimeZone("Asia/Shanghai")
        val date = BaZiSessionViewModel.composeBirthDate(2000, 1, 1, 23, tz)
        assertNotNull(date)
        val cal = Calendar.getInstance(tz).apply { time = date!! }
        assertEquals(23, cal.get(Calendar.HOUR_OF_DAY))
        val day = GanzhiCalendar.dayStemBranch(
            cal.get(Calendar.YEAR),
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.HOUR_OF_DAY),
            tz,
        )
        assertEquals("己未", day.name)
    }

    @Test
    fun daysInMonthFebruary() {
        assertEquals(28, BaZiSessionViewModel.daysInMonth(2023, 2))
        assertEquals(29, BaZiSessionViewModel.daysInMonth(2024, 2))
        assertEquals(31, BaZiSessionViewModel.daysInMonth(2024, 1))
        assertTrue(BaZiSessionViewModel.SHICHEN_OPTIONS.size == 12)
    }
}
