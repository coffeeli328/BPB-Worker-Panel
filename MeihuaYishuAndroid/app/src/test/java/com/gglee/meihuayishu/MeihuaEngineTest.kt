package com.gglee.meihuayishu

import com.gglee.meihuayishu.engine.MeihuaEngine
import com.gglee.meihuayishu.engine.TiYongRelation
import com.gglee.meihuayishu.engine.Trigram
import com.gglee.meihuayishu.engine.WuXing
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class MeihuaEngineTest {

    @Test
    fun mod8_mapsRemainderZeroToKun() {
        assertEquals(8, MeihuaEngine.mod8(8))
        assertEquals(8, MeihuaEngine.mod8(16))
        assertEquals(1, MeihuaEngine.mod8(1))
        assertEquals(3, MeihuaEngine.mod8(11))
    }

    @Test
    fun mod6_mapsRemainderZeroToSix() {
        assertEquals(6, MeihuaEngine.mod6(6))
        assertEquals(6, MeihuaEngine.mod6(12))
        assertEquals(1, MeihuaEngine.mod6(1))
    }

    @Test
    fun trigramFromNumber() {
        assertEquals(Trigram.QIAN, Trigram.fromNumber(1))
        assertEquals(Trigram.KUN, Trigram.fromNumber(8))
        assertEquals(Trigram.KUN, Trigram.fromNumber(16))
        assertEquals(Trigram.LI, Trigram.fromNumber(3))
    }

    @Test
    fun numberCast_knownCase() {
        // 上 1 乾，下 8 坤，动爻 1+8=9 → 3
        val chart = MeihuaEngine.castByNumbers("测试", "1", "8", "")
        assertEquals(Trigram.QIAN, chart.original.upper)
        assertEquals(Trigram.KUN, chart.original.lower)
        assertEquals(3, chart.movingLine)
        // 动在下卦 → 体=上乾，用=下坤
        assertEquals(Trigram.QIAN, chart.ti)
        assertEquals(Trigram.KUN, chart.yong)
        // 用坤土生体乾金
        assertEquals(TiYongRelation.YONG_SHENG_TI, chart.tiYongRelation)
        assertEquals("天地否", chart.original.name)
    }

    @Test
    fun mutualAndChanged() {
        val chart = MeihuaEngine.castManual("测", upperNum = 1, lowerNum = 1, moving = 1)
        // 乾为天，初爻动 → 下卦变巽，成天风姤
        assertEquals("乾为天", chart.original.name)
        assertEquals(Trigram.XUN, chart.changed.lower)
        assertEquals(Trigram.QIAN, chart.changed.upper)
        assertEquals("天风姤", chart.changed.name)
        // 乾六爻皆阳，互卦仍乾
        assertEquals("乾为天", chart.mutual.name)
    }

    @Test
    fun tiYong_upperMovingUsesLowerAsTi() {
        val chart = MeihuaEngine.castManual("", 3, 6, 5)
        // 离上坎下=既济，动5在上 → 体=下坎，用=上离
        assertEquals(Trigram.KAN, chart.ti)
        assertEquals(Trigram.LI, chart.yong)
        // 体坎水克用离火
        assertEquals(TiYongRelation.TI_KE_YONG, chart.tiYongRelation)
    }

    @Test
    fun wuXingRelations() {
        assertEquals(WuXing.SHUI, WuXing.JIN.generates())
        assertEquals(TiYongRelation.YONG_SHENG_TI, WuXing.MU.relationTo(WuXing.SHUI)) // 水生木：用生？ body木 vs other水 → other generates body → YONG_SHENG_TI
        assertEquals(TiYongRelation.TI_KE_YONG, WuXing.JIN.relationTo(WuXing.MU))
    }

    @Test
    fun timeCast_deterministic() {
        val tz = TimeZone.getTimeZone("Asia/Shanghai")
        val cal = Calendar.getInstance(tz).apply {
            set(2024, Calendar.MARCH, 15, 10, 30, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val a = MeihuaEngine.castByTime("问", cal.time, tz)
        val b = MeihuaEngine.castByTime("问", cal.time, tz)
        assertEquals(a.upperNumber, b.upperNumber)
        assertEquals(a.lowerNumber, b.lowerNumber)
        assertEquals(a.movingLine, b.movingLine)
        assertTrue(a.ganzhiLine.isNotBlank())
        assertTrue(a.interpretations.isNotEmpty())
    }

    @Test
    fun hourBranch() {
        assertEquals(1, MeihuaEngine.hourBranchNumber(23))
        assertEquals(1, MeihuaEngine.hourBranchNumber(0))
        assertEquals(2, MeihuaEngine.hourBranchNumber(1))
        assertEquals(7, MeihuaEngine.hourBranchNumber(12))
    }
}
