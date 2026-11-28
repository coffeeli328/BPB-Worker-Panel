package com.gglee.ziweidoushu

import com.gglee.ziweidoushu.engine.EarthlyBranch
import com.gglee.ziweidoushu.engine.HeavenlyStem
import com.gglee.ziweidoushu.engine.LunarCalendar
import com.gglee.ziweidoushu.engine.StemBranch
import org.junit.Assert.assertEquals
import org.junit.Test

/** 八字四柱黄金用例（立春换年、节气月、日柱锚定、五鼠遁时）。 */
class BaZiTest {

    @Test
    fun dayPillar_anchors() {
        assertEquals("甲戌", dayName(1900, 1, 1, 12))
        assertEquals("戊午", dayName(2000, 1, 1, 12))
        assertEquals("甲子", dayName(2024, 1, 1, 12))
    }

    @Test
    fun yearPillar_lichunBoundary() {
        // 1984 立春约 2/4：之前仍为癸亥，之后甲子
        assertEquals("癸亥", yearName(1984, 2, 3, 12))
        assertEquals("甲子", yearName(1984, 2, 4, 12))
        assertEquals("庚午", yearName(1990, 5, 15, 10))
    }

    @Test
    fun fullPillars_1990_05_15_siShi() {
        val g = LunarCalendar.fourPillars(1990, 5, 15, 10)
        assertEquals("庚午", g.year.name)
        assertEquals("辛巳", g.month.name) // 立夏后巳月，庚年五虎遁起戊 +3 = 辛
        assertEquals("庚辰", g.day.name)
        assertEquals("辛巳", g.hour.name) // 巳时，庚日五鼠遁起丙 +5 = 辛
    }

    @Test
    fun lateZiHour_rollsDay() {
        // 23 点起为次日子时
        val g = LunarCalendar.fourPillars(2000, 1, 1, 23)
        assertEquals("己未", g.day.name) // 次日 1/2 己未
        assertEquals(EarthlyBranch.ZI, g.hour.branch)
        assertEquals(HeavenlyStem.JIA, g.hour.stem) // 己日起甲子时
    }

    @Test
    fun monthPillar_jingzheBoundary() {
        // 近似节气表：3/6 惊蛰起卯月；3/5 仍寅月
        val before = LunarCalendar.fourPillars(1990, 3, 5, 12)
        val after = LunarCalendar.fourPillars(1990, 3, 6, 12)
        assertEquals("戊寅", before.month.name)
        assertEquals("己卯", after.month.name)
    }

    private fun dayName(y: Int, m: Int, d: Int, h: Int) =
        LunarCalendar.fourPillars(y, m, d, h).day.name

    private fun yearName(y: Int, m: Int, d: Int, h: Int) =
        LunarCalendar.fourPillars(y, m, d, h).year.name
}
