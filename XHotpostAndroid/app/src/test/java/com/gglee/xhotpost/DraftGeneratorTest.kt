package com.gglee.xhotpost

import com.gglee.xhotpost.domain.AppSettings
import com.gglee.xhotpost.domain.DraftGenerator
import com.gglee.xhotpost.domain.HotTopic
import org.junit.Assert.assertTrue
import org.junit.Test

class DraftGeneratorTest {
    @Test
    fun templateDraft_respects280Limit() {
        val topic = HotTopic(
            id = "1",
            title = "测试热点标题",
            summary = "摘要内容",
            source = "demo",
            url = null,
            score = 90,
            language = "zh",
            fetchedAt = 0L,
        )
        val text = DraftGenerator.templateDraft(topic, AppSettings())
        assertTrue(text.length <= 280)
        assertTrue(text.contains("【热点】"))
    }
}
