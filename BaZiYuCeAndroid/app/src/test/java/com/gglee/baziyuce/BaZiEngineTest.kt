package com.gglee.baziyuce

import com.gglee.baziyuce.engine.BaZiEngine
import com.gglee.baziyuce.engine.BranchRelations
import com.gglee.baziyuce.engine.EarthlyBranch
import com.gglee.baziyuce.engine.GanzhiCalendar
import com.gglee.baziyuce.engine.Gender
import com.gglee.baziyuce.engine.HeavenlyStem
import com.gglee.baziyuce.engine.PredictionEngine
import com.gglee.baziyuce.engine.TenGod
import com.gglee.baziyuce.engine.TenGods
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class BaZiEngineTest {

    private val tz: TimeZone = TimeZone.getTimeZone("Asia/Shanghai")

    @Test
    fun dayPillarKnownAnchors() {
        val d1900 = GanzhiCalendar.dayStemBranch(1900, 1, 1, 12, tz)
        assertEquals("甲戌", d1900.name)

        val d2000 = GanzhiCalendar.dayStemBranch(2000, 1, 1, 12, tz)
        assertEquals("戊午", d2000.name)
    }

    @Test
    fun ziHourRollsDay() {
        val before = GanzhiCalendar.dayStemBranch(2000, 1, 1, 22, tz)
        val after = GanzhiCalendar.dayStemBranch(2000, 1, 1, 23, tz)
        assertEquals("戊午", before.name)
        assertEquals("己未", after.name)
    }

    @Test
    fun hourStemWuShuDun() {
        val h = GanzhiCalendar.hourStemBranch(HeavenlyStem.JIA, 0)
        assertEquals("甲子", h.name)
        val h2 = GanzhiCalendar.hourStemBranch(HeavenlyStem.JIA, 10)
        assertEquals(EarthlyBranch.SI, h2.branch)
        assertEquals(HeavenlyStem.JI, h2.stem)
    }

    @Test
    fun tenGodsRelativeToDayMaster() {
        assertEquals(TenGod.JIE_CAI, TenGods.of(HeavenlyStem.YI, HeavenlyStem.JIA))
        assertEquals(TenGod.SHI_SHEN, TenGods.of(HeavenlyStem.BING, HeavenlyStem.JIA))
        assertEquals(TenGod.PIAN_CAI, TenGods.of(HeavenlyStem.WU, HeavenlyStem.JIA))
        assertEquals(TenGod.QI_SHA, TenGods.of(HeavenlyStem.GENG, HeavenlyStem.JIA))
        assertEquals(TenGod.PIAN_YIN, TenGods.of(HeavenlyStem.REN, HeavenlyStem.JIA))
        assertEquals(TenGod.BI_JIAN, TenGods.of(HeavenlyStem.JIA, HeavenlyStem.JIA))
    }

    @Test
    fun chartAndForecastWireUp() {
        val cal = Calendar.getInstance(tz)
        cal.set(1990, Calendar.MAY, 15, 10, 0, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val birth = cal.time
        val chart = BaZiEngine.chart(birthDate = birth, gender = Gender.MALE, timeZone = tz)
        assertEquals(4, chart.pillars.size)
        assertFalse(chart.fourPillarsText.isEmpty())

        val forecasts = PredictionEngine.allForecasts(chart = chart, at = birth, timeZone = tz)
        assertEquals(3, forecasts.size)
        for (f in forecasts) {
            assertFalse(f.summary.isEmpty())
            assertFalse(f.tips.isEmpty())
            assertTrue(f.score in 25..95)
        }
    }

    @Test
    fun branchClash() {
        val notes = BranchRelations.describe(EarthlyBranch.ZI, listOf(EarthlyBranch.WU, EarthlyBranch.MAO))
        assertTrue(notes.any { "冲" in it })
    }
}
