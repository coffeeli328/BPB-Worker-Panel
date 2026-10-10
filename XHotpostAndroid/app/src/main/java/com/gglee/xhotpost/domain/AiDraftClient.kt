package com.gglee.xhotpost.domain

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * OpenAI-compatible Chat Completions client (OpenAI / DeepSeek / 通义 compatible 网关等).
 * Failures return null so the caller can fall back to local templates.
 */
class AiDraftClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build(),
) {
    fun generate(topic: HotTopic, settings: AppSettings): String? {
        if (!settings.aiEnabled) return null
        val key = settings.aiApiKey.trim()
        if (key.isEmpty()) return null

        val base = settings.aiBaseUrl.trim().trimEnd('/')
        if (base.isEmpty()) return null
        val url = if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
        val model = settings.aiModel.trim().ifBlank { "gpt-4o-mini" }

        val body = JSONObject()
            .put("model", model)
            .put(
                "messages",
                JSONArray()
                    .put(
                        JSONObject()
                            .put("role", "system")
                            .put("content", DraftGenerator.aiSystemPrompt(settings)),
                    )
                    .put(
                        JSONObject()
                            .put("role", "user")
                            .put("content", DraftGenerator.aiUserPrompt(topic)),
                    ),
            )
            .put("temperature", 0.85)
            .put("max_tokens", 400)

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $key")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val raw = response.body?.string().orEmpty()
                if (raw.isBlank()) return null
                val text = extractContent(raw) ?: return null
                val cleaned = DraftGenerator.stripExcess(
                    text
                        .trim()
                        .removePrefix("\"")
                        .removeSuffix("\"")
                        .trim(),
                )
                cleaned.takeIf { it.length >= 12 }
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun extractContent(raw: String): String? {
        return try {
            val root = JSONObject(raw)
            val choices = root.optJSONArray("choices") ?: return null
            if (choices.length() == 0) return null
            val message = choices.getJSONObject(0).optJSONObject("message")
            message?.optString("content")?.takeIf { it.isNotBlank() }
                ?: choices.getJSONObject(0).optString("text").takeIf { it.isNotBlank() }
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()
    }
}
