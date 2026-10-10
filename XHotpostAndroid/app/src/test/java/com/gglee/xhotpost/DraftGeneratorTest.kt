package com.gglee.xhotpost

import com.gglee.xhotpost.domain.AppSettings
import com.gglee.xhotpost.domain.DraftGenerator
import com.gglee.xhotpost.domain.HotTopic
import com.gglee.xhotpost.domain.WritingStyle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftGeneratorTest {
    private fun topic(
        id: String = "1",
        title: String = "开源模型掀起新一轮 AI 应用潮",
        summary: String = "开发者用开源模型快速搭产品，成本下降，竞争转向落地场景。",
    ) = HotTopic(
        id = id,
        title = title,
        summary = summary,
        source = "demo",
        url = null,
        score = 90,
        language = "zh",
        fetchedAt = 0L,
    )

    @Test
    fun templateDraft_respects280Limit() {
        val text = DraftGenerator.templateDraft(topic(), AppSettings())
        assertTrue(text.length <= 280)
    }

    @Test
    fun templateDraft_avoidsOldHotLabelTemplate() {
        val text = DraftGenerator.templateDraft(topic(), AppSettings())
        assertFalse(text.startsWith("【热点】"))
        assertTrue(text.contains("开源模型") || text.contains("落地"))
    }

    @Test
    fun differentStyles_produceDifferentBodies() {
        val t = topic(id = "style-seed")
        val opinion = DraftGenerator.templateDraft(
            t,
            AppSettings(writingStyle = WritingStyle.OPINION),
        )
        val howto = DraftGenerator.templateDraft(
            t,
            AppSettings(writingStyle = WritingStyle.HOWTO),
        )
        val casual = DraftGenerator.templateDraft(
            t,
            AppSettings(writingStyle = WritingStyle.CASUAL),
        )
        assertTrue(opinion != howto || howto != casual)
        assertTrue(howto.contains("步") || howto.contains("清单") || howto.contains("操作") || howto.contains("资产"))
    }

    @Test
    fun personaAndAffiliate_areApplied() {
        val settings = AppSettings(
            writingStyle = WritingStyle.CASUAL,
            persona = "犀利但真诚",
            affiliateUrl = "https://example.com/x",
            ctaTemplate = "详情：{link}",
        )
        val text = DraftGenerator.templateDraft(topic(), settings)
        assertTrue(text.contains("https://example.com/x"))
        assertTrue(text.length <= 280)
    }

    @Test
    fun englishMode_usesEnglishBody() {
        val text = DraftGenerator.templateDraft(
            topic().copy(language = "en", title = "Open models surge", summary = "Costs drop."),
            AppSettings(language = com.gglee.xhotpost.domain.ContentLanguage.EN),
        )
        assertTrue(text.length <= 280)
        assertTrue(text.contains("Open models") || text.contains("Costs"))
    }
}
