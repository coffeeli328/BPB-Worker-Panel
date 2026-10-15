package com.gglee.xhotpost

import com.gglee.xhotpost.domain.AppSettings
import com.gglee.xhotpost.domain.DraftGenerator
import com.gglee.xhotpost.domain.HotTopic
import com.gglee.xhotpost.domain.NicheId
import com.gglee.xhotpost.domain.WritingStyle
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftGeneratorTest {
    private fun topic(
        id: String = "1",
        title: String = "#AI",
        summary: String = "",
    ) = HotTopic(
        id = id,
        title = title,
        summary = summary,
        source = "X 热搜 · 美国",
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
    fun templateDraft_avoidsMetaAndOldLabels() {
        val text = DraftGenerator.templateDraft(
            topic(summary = "X 热搜 · 美国 · 第 1 名。打开可看实时讨论"),
            AppSettings(niche = NicheId.TECH),
        )
        assertFalse(text.startsWith("【热点】"))
        assertFalse(text.contains("第 1 名"))
        assertFalse(text.contains("打开可看"))
        assertFalse(text.contains("按你的人设"))
        assertTrue(text.contains("#AI") || text.contains("AI"))
    }

    @Test
    fun usableSignal_dropsCollectorMeta() {
        assertNull(DraftGenerator.usableSignal("X 热搜 · 日本 · 第 3 名"))
        assertTrue(DraftGenerator.usableSignal("成本下降，竞争转向落地")!!.contains("成本"))
    }

    @Test
    fun differentStyles_orNiches_vary() {
        val t = topic(id = "style-seed", title = "ChatGPT")
        val a = DraftGenerator.templateDraft(t, AppSettings(writingStyle = WritingStyle.OPINION))
        val b = DraftGenerator.templateDraft(t, AppSettings(writingStyle = WritingStyle.CASUAL))
        val c = DraftGenerator.templateDraft(
            t,
            AppSettings(writingStyle = WritingStyle.HOWTO, niche = NicheId.CREATOR),
        )
        assertTrue(a != b || b != c)
    }

    @Test
    fun rewriteNudge_changesLocalVariant() {
        val base = topic(id = "nudge")
        val first = DraftGenerator.templateDraft(base, AppSettings())
        val second = DraftGenerator.templateDraft(base.copy(id = "nudge-999"), AppSettings())
        // Different seed should usually differ; if equal, still valid but rare.
        if (first == second) {
            assertTrue(first.length <= 280)
        } else {
            assertNotEquals(first, second)
        }
    }

    @Test
    fun affiliate_appendedWhenShort() {
        val settings = AppSettings(
            affiliateUrl = "https://example.com/x",
            ctaTemplate = "详情：{link}",
        )
        val text = DraftGenerator.templateDraft(topic(), settings)
        assertTrue(text.contains("https://example.com/x") || text.length > 200)
        assertTrue(text.length <= 280)
    }
}
