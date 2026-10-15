package com.gglee.xhotpost

import com.gglee.xhotpost.domain.AppSettings
import com.gglee.xhotpost.domain.NicheId
import com.gglee.xhotpost.domain.TrendCollector
import com.gglee.xhotpost.domain.XTrendRegion
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TrendCollectorTest {
    private val sampleHtml = """
        <table class="table">
        <tr><th class="pos">1</th><td class="main"><a href="/united-states/trend/%23AI/">#AI</a></td></tr>
        <tr><th class="pos">2</th><td class="main"><a class="string" href="/united-states/trend/Ukraine/">Ukraine</a></td></tr>
        <tr><th class="pos">3</th><td class="main"><a href="/united-states/trend/ChatGPT/">ChatGPT</a></td></tr>
        </table>
    """.trimIndent()

    @Test
    fun parseGetDayTrends_extractsTitlesAndXLinks() {
        val topics = TrendCollector().parseGetDayTrends(sampleHtml, "united-states", 0L)
        assertEquals(3, topics.size)
        assertEquals("#AI", topics[0].title)
        assertTrue(topics[0].source.contains("X 热搜"))
        assertTrue(topics[0].url!!.startsWith("https://x.com/search?"))
        assertEquals(100, topics[0].score)
    }

    @Test
    fun collect_inDemoMode_returnsXDemoTopics() {
        val topics = TrendCollector().collect(AppSettings(demoMode = true, niche = NicheId.LOCAL))
        assertTrue(topics.isNotEmpty())
        assertTrue(topics.any { it.source.contains("X") })
    }

    @Test
    fun collect_injectsWatchTopics() {
        val topics = TrendCollector().collect(
            AppSettings(
                demoMode = true,
                niche = NicheId.LOCAL,
                watchTopics = listOf("温哥华房价", "#创作者经济"),
            ),
        )
        assertTrue(topics.any { it.title == "温哥华房价" && it.source == "自选话题" })
        assertTrue(topics.any { it.title == "#创作者经济" && it.source == "自选话题" })
    }

    @Test
    fun xSearchUrl_encodesQuery() {
        val url = TrendCollector.xSearchUrl("#AI 大模型")
        assertTrue(url.contains("x.com/search"))
        assertTrue(url.contains("%23AI") || url.contains("AI"))
    }

    @Test
    fun regionEnum_hasSlugsForSelectableAreas() {
        assertEquals("worldwide", XTrendRegion.WORLDWIDE.slug)
        assertEquals("canada", XTrendRegion.CANADA.slug)
        assertEquals("south-korea", XTrendRegion.SOUTH_KOREA.slug)
        assertEquals(null, XTrendRegion.AUTO.slug)
        assertNotNull(XTrendRegion.entries.find { it.labelZh == "日本" })
        assertTrue(XTrendRegion.entries.size >= 10)
    }
}
