package com.gglee.qimendunjia

import com.gglee.qimendunjia.engine.EightGate
import com.gglee.qimendunjia.engine.NineStar
import com.gglee.qimendunjia.engine.Palace
import com.gglee.qimendunjia.engine.QimenEngine
import com.gglee.qimendunjia.engine.QuestionTopic
import com.gglee.qimendunjia.engine.StemBranch
import org.junit.Assert.assertEquals
import org.junit.Test

class PlateEngineTest {

    @Test
    fun yangDun1JiaZi_zhiFuPeng_zhiShiXiu_zhiFuPalaceKan1() {
        val hour = StemBranch.parse("甲子")!!
        val plate = QimenEngine.buildPlate(isYangDun = true, juNumber = 1, hour = hour)
        assertEquals(NineStar.PENG, plate.zhiFuStar)
        assertEquals(EightGate.XIU, plate.zhiShiGate)
        assertEquals(Palace.KAN1, plate.zhiFuPalace)
        assertEquals(Palace.KAN1, plate.zhiShiPalace)
    }

    @Test
    fun yangDun1DingMao_zhiFuPalaceDui7_zhiShiPalaceXun4() {
        val hour = StemBranch.parse("丁卯")!!
        val plate = QimenEngine.buildPlate(isYangDun = true, juNumber = 1, hour = hour)
        assertEquals(Palace.DUI7, plate.zhiFuPalace)
        assertEquals(Palace.XUN4, plate.zhiShiPalace)
    }

    @Test
    fun questionTopic_detect求財_mapsToWealth() {
        assertEquals(QuestionTopic.WEALTH, QuestionTopic.detect("求财"))
    }
}
