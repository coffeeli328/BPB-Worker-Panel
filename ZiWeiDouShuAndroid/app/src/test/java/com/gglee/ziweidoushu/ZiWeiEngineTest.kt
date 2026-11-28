package com.gglee.ziweidoushu

import com.gglee.ziweidoushu.engine.CastRequest
import com.gglee.ziweidoushu.engine.EarthlyBranch
import com.gglee.ziweidoushu.engine.Gender
import com.gglee.ziweidoushu.engine.HeavenlyStem
import com.gglee.ziweidoushu.engine.LunarCalendar
import com.gglee.ziweidoushu.engine.PalaceType
import com.gglee.ziweidoushu.engine.ScopeProjector
import com.gglee.ziweidoushu.engine.SiHua
import com.gglee.ziweidoushu.engine.SiHuaResolver
import com.gglee.ziweidoushu.engine.Star
import com.gglee.ziweidoushu.engine.StemBranch
import com.gglee.ziweidoushu.engine.ZiWeiEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ZiWeiEngineTest {

    @Test
    fun mingPalace_month1_hourZi_isYin() {
        // 正月子时：月宫在寅，逆数子=0 → 命宫寅
        assertEquals(EarthlyBranch.YIN, ZiWeiEngine.locateMingPalace(1, EarthlyBranch.ZI))
    }

    @Test
    fun mingPalace_month1_hourWu() {
        // 正月午时：从寅逆数 6 → 申
        assertEquals(EarthlyBranch.SHEN, ZiWeiEngine.locateMingPalace(1, EarthlyBranch.WU))
    }

    @Test
    fun shenPalace_month1_hourWu() {
        // 正月午时：从寅顺数 6 → 申
        assertEquals(EarthlyBranch.SHEN, ZiWeiEngine.locateShenPalace(1, EarthlyBranch.WU))
    }

    @Test
    fun ziweiOffset_day1_ju2() {
        // 1/2 → remainder 1 → quotient becomes 1 …
        val offset = ZiWeiEngine.locateZiweiOffset(1, 2)
        assertTrue(offset in 0..11)
    }

    @Test
    fun palaceStems_jiaYear_yinIsBing() {
        val stems = ZiWeiEngine.palaceStems(HeavenlyStem.JIA)
        assertEquals(HeavenlyStem.BING, stems[EarthlyBranch.YIN.rawValue])
    }

    @Test
    fun nayin_jiaZi_isMetal() {
        assertEquals(0, ZiWeiEngine.nayinElement(StemBranch(HeavenlyStem.JIA, EarthlyBranch.ZI)))
    }

    @Test
    fun lunar_knownDate_19900215() {
        // 1990-01-27 solar ≈ 农历己巳年正月初一附近；用引擎自洽即可
        val lunar = LunarCalendar.solarToLunar(1990, 1, 27)
        assertEquals(1990, lunar.year)
        assertEquals(1, lunar.month)
        assertEquals(1, lunar.day)
    }

    @Test
    fun cast_producesTwelvePalacesAndMajors() {
        val chart = ZiWeiEngine.cast(
            CastRequest(
                name = "测试",
                gender = Gender.MALE,
                year = 1990,
                month = 5,
                day = 15,
                hour = 10,
                useLunar = false,
            ),
        )
        assertEquals(12, chart.cells.size)
        assertEquals(12, chart.decades.size)
        assertNotNull(chart.cell(PalaceType.MING))
        val majors = chart.cells.flatMap { it.majorStars.map { s -> s.star } }.toSet()
        assertTrue(majors.contains(Star.ZI_WEI))
        assertTrue(majors.contains(Star.TIAN_FU))
        assertTrue(chart.interpretations.isNotEmpty())
        // 紫微与天府相对寅偏移互补
        val zw = (chart.ziweiBranch.rawValue - EarthlyBranch.YIN.rawValue + 12) % 12
        val tf = chart.cells.first { c -> c.stars.any { it.star == Star.TIAN_FU } }.branch
        val tfOff = (tf.rawValue - EarthlyBranch.YIN.rawValue + 12) % 12
        assertEquals(0, (zw + tfOff) % 12)
    }

    @Test
    fun lunarRoundTrip_sample() {
        val solar = LunarCalendar.lunarToSolar(2000, 1, 1, false)
        val back = LunarCalendar.solarToLunar(solar.year, solar.month, solar.day)
        assertEquals(2000, back.year)
        assertEquals(1, back.month)
        assertEquals(1, back.day)
    }

    @Test
    fun siHua_jia_mapsClassicStars() {
        val map = SiHuaResolver.starsFor(HeavenlyStem.JIA)
        assertEquals(Star.LIAN_ZHEN, map[SiHua.LU])
        assertEquals(Star.PO_JUN, map[SiHua.QUAN])
        assertEquals(Star.WU_QU, map[SiHua.KE])
        assertEquals(Star.TAI_YANG, map[SiHua.JI])
    }

    @Test
    fun cast_includesNatalSiHuaOnStars() {
        val chart = ZiWeiEngine.cast(
            CastRequest(year = 1984, month = 6, day = 1, hour = 8, gender = Gender.MALE),
        )
        // 1984 甲子年
        assertEquals(HeavenlyStem.JIA, chart.yearSB.stem)
        assertEquals(4, chart.natalSiHua.size)
        assertTrue(chart.cells.any { c -> c.stars.any { it.natalSiHua == SiHua.LU } })
        assertTrue(chart.cells.any { c -> c.stars.any { it.natalSiHua == SiHua.JI } })
    }

    @Test
    fun annualScope_renamesMingToYearBranch() {
        val chart = ZiWeiEngine.cast(
            CastRequest(year = 1990, month = 5, day = 15, hour = 10, gender = Gender.FEMALE),
        )
        val sb = ZiWeiEngine.annualStemBranch(2024)
        val (cells, overlay) = ScopeProjector.annual(chart, sb, 2024)
        val focus = cells.first { it.branch == sb.branch }
        assertEquals(PalaceType.MING, focus.scopeType)
        assertEquals(sb.branch, overlay.focusBranch)
        assertEquals(4, overlay.siHua.size)
    }

    @Test
    fun decadeScope_usesPalaceStemSiHua() {
        val chart = ZiWeiEngine.cast(
            CastRequest(year = 1990, month = 5, day = 15, hour = 10, gender = Gender.MALE),
        )
        val decade = chart.decades[2]
        val (_, overlay) = ScopeProjector.decade(chart, decade)
        assertEquals(chart.cellByBranch(decade.branch).stem, overlay.stem)
        assertTrue(overlay.siHua.isNotEmpty())
    }
}
