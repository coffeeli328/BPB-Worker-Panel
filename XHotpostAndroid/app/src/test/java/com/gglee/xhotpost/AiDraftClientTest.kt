package com.gglee.xhotpost

import com.gglee.xhotpost.domain.AiDraftClient
import com.gglee.xhotpost.domain.AppSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiDraftClientTest {
    @Test
    fun resolveChatUrl_addsV1ForDeepSeekRoot() {
        val url = AiDraftClient.resolveChatUrl("https://api.deepseek.com")
        assertEquals("https://api.deepseek.com/v1/chat/completions", url)
    }

    @Test
    fun resolveChatUrl_keepsExistingV1() {
        val url = AiDraftClient.resolveChatUrl("https://api.deepseek.com/v1")
        assertEquals("https://api.deepseek.com/v1/chat/completions", url)
    }

    @Test
    fun resolveModel_overridesGptOnDeepSeek() {
        val settings = AppSettings(
            aiBaseUrl = AiDraftClient.DEEPSEEK_BASE,
            aiModel = "gpt-4o-mini",
        )
        assertEquals("deepseek-chat", AiDraftClient.resolveModel(settings))
    }

    @Test
    fun looksLikeDeepSeekMisconfig_detectsOpenaiDefaults() {
        val settings = AppSettings(
            aiApiKey = "sk-test",
            aiBaseUrl = "https://api.openai.com/v1",
            aiModel = "gpt-4o-mini",
        )
        assertTrue(AiDraftClient.looksLikeDeepSeekMisconfig(settings))
    }
}
