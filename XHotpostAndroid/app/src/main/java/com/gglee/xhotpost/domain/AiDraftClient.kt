package com.gglee.xhotpost.domain

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class AiDraftResult {
    data class Ok(val text: String) : AiDraftResult()
    data class Err(val message: String) : AiDraftResult()
    data object Skipped : AiDraftResult()
}

/**
 * OpenAI-compatible Chat Completions (OpenAI / DeepSeek / 通义兼容网关等).
 */
class AiDraftClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build(),
) {
    fun generate(topic: HotTopic, settings: AppSettings): AiDraftResult {
        val key = settings.aiApiKey.trim()
        if (!settings.aiEnabled || key.isEmpty()) return AiDraftResult.Skipped

        val url = resolveChatUrl(settings.aiBaseUrl)
            ?: return AiDraftResult.Err("API Base URL 无效")
        val model = resolveModel(settings)

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
            .put("temperature", 0.9)
            .put("max_tokens", 400)

        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer $key")
            .header("Content-Type", "application/json")
            .post(body.toString().toRequestBody(JSON_MEDIA))
            .build()

        return try {
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return AiDraftResult.Err(
                        "HTTP ${response.code}: ${shortError(raw).ifBlank { response.message }}",
                    )
                }
                if (raw.isBlank()) return AiDraftResult.Err("API 返回空内容")
                val text = extractContent(raw)
                    ?: return AiDraftResult.Err("无法解析 API 响应")
                val cleaned = DraftGenerator.stripExcess(
                    text.trim()
                        .removePrefix("```")
                        .removeSuffix("```")
                        .trim()
                        .removePrefix("\"")
                        .removeSuffix("\"")
                        .trim(),
                )
                if (cleaned.length < 8) {
                    return AiDraftResult.Err("AI 输出过短")
                }
                AiDraftResult.Ok(cleaned)
            }
        } catch (e: Exception) {
            AiDraftResult.Err(e.message?.take(120) ?: e.javaClass.simpleName)
        }
    }

    /** Lightweight connectivity check for settings UI. */
    fun ping(settings: AppSettings): AiDraftResult {
        val probe = HotTopic(
            id = "ping",
            title = "#AI",
            summary = "连通性测试",
            source = "ping",
            url = null,
            score = 1,
            language = "zh",
            fetchedAt = System.currentTimeMillis(),
        )
        return generate(probe, settings)
    }

    companion object {
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        const val DEEPSEEK_BASE = "https://api.deepseek.com/v1"
        const val DEEPSEEK_MODEL = "deepseek-chat"
        const val OPENAI_BASE = "https://api.openai.com/v1"

        fun resolveChatUrl(rawBase: String): String? {
            var base = rawBase.trim().trimEnd('/')
            if (base.isEmpty()) return null
            // DeepSeek docs accept api.deepseek.com — ensure /v1 for chat completions.
            if (base.equals("https://api.deepseek.com", ignoreCase = true) ||
                base.equals("http://api.deepseek.com", ignoreCase = true)
            ) {
                base = "$base/v1"
            }
            return if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
        }

        fun resolveModel(settings: AppSettings): String {
            val model = settings.aiModel.trim()
            val base = settings.aiBaseUrl.lowercase()
            if (model.isNotBlank() &&
                !(base.contains("deepseek") && model.startsWith("gpt-", ignoreCase = true))
            ) {
                return model
            }
            return if (base.contains("deepseek")) DEEPSEEK_MODEL else "gpt-4o-mini"
        }

        fun looksLikeDeepSeekMisconfig(settings: AppSettings): Boolean {
            if (settings.aiApiKey.isBlank()) return false
            val base = settings.aiBaseUrl.lowercase()
            val model = settings.aiModel.lowercase()
            // Key set but still on OpenAI defaults — common DeepSeek misconfig.
            return base.contains("openai.com") &&
                (model.isBlank() || model.startsWith("gpt-"))
        }

        private fun shortError(raw: String): String {
            return try {
                val root = JSONObject(raw)
                val err = root.optJSONObject("error")
                val msg = err?.optString("message")
                    ?: root.optString("message")
                    ?: raw
                msg.replace(Regex("\\s+"), " ").take(160)
            } catch (_: Exception) {
                raw.replace(Regex("\\s+"), " ").take(160)
            }
        }

        private fun extractContent(raw: String): String? {
            return try {
                val root = JSONObject(raw)
                val choices = root.optJSONArray("choices") ?: return null
                if (choices.length() == 0) return null
                val first = choices.getJSONObject(0)
                val message = first.optJSONObject("message")
                message?.optString("content")?.takeIf { it.isNotBlank() }
                    ?: first.optString("text").takeIf { it.isNotBlank() }
            } catch (_: Exception) {
                null
            }
        }
    }
}
