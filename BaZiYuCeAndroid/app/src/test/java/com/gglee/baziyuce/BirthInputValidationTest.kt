package com.gglee.baziyuce

import com.gglee.baziyuce.engine.BaZiEngine
import com.gglee.baziyuce.engine.EarthlyBranch
import com.gglee.baziyuce.engine.GanzhiCalendar
import com.gglee.baziyuce.engine.Gender
import com.gglee.baziyuce.engine.ShiChen
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class BirthInputValidationTest {

    @Test
    fun shiChenMatchesIosRangesAndZiHour23() {
        assertEquals(12, ShiChen.all.size)
        val zi = ShiChen.all[EarthlyBranch.ZI.ordinal]
        assertEquals(23, zi.representativeHour)
        assertEquals("子时（23–1点）", zi.displayName)
        val si = ShiChen.all[EarthlyBranch.SI.ordinal]
        assertEquals(9, si.representativeHour)
    }

    @Test
    fun sessionValidationMessagesMatchIos() {
        val vm = BaZiSessionViewModel()
        vm.updateBirthYear(1899)
        assertFalse(vm.validateAndComposeBirthDate())
        assertEquals("年份请在 1900–2100 之间", vm.inputError)

        // Like iOS onChange clamp: day is clamped when month/year changes
        vm.updateBirthYear(2023)
        vm.updateBirthMonth(1)
        vm.updateBirthDay(31)
        vm.updateBirthMonth(2)
        assertEquals(28, vm.birthDay)
        assertTrue(vm.validateAndComposeBirthDate())

        vm.updateBirthYear(2024)
        vm.updateBirthMonth(2)
        vm.updateBirthDay(29)
        assertEquals(29, vm.birthDay)
        assertTrue(vm.validateAndComposeBirthDate())
    }

    @Test
    fun ziShiChenComposesHour23AndRollsDayPillar() {
        val tz = TimeZone.getTimeZone("Asia/Shanghai")
        val vm = BaZiSessionViewModel()
        // Use reflection-free path: update fields then calculate with forced TZ via engine
        vm.updateBirthYear(2000)
        vm.updateBirthMonth(1)
        vm.updateBirthDay(1)
        vm.updateBirthShiChenIndex(EarthlyBranch.ZI.ordinal)
        assertTrue(vm.validateAndComposeBirthDate())

        // Compose explicitly in Asia/Shanghai for engine parity check
        val dateCal = Calendar.getInstance(tz)
        dateCal.clear()
        dateCal.set(2000, Calendar.JANUARY, 1, 23, 0, 0)
        dateCal.set(Calendar.MILLISECOND, 0)
        val day = GanzhiCalendar.dayStemBranch(2000, 1, 1, 23, tz)
        assertEquals("己未", day.name)
        val chart = BaZiEngine.chart(birthDate = dateCal.time, gender = Gender.MALE, timeZone = tz)
        assertEquals("己未", chart.day.name)
    }

    @Test
    fun daysInMonthFebruary() {
        assertEquals(28, BaZiSessionViewModel.daysInMonth(2023, 2))
        assertEquals(29, BaZiSessionViewModel.daysInMonth(2024, 2))
        assertEquals(31, BaZiSessionViewModel.daysInMonth(2024, 1))
    }
}
