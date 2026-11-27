package com.gglee.ziweidoushu.ai

import com.gglee.ziweidoushu.engine.ZiWeiChart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

sealed class AiError(message: String) : Exception(message) {
    class NotConfigured : AiError("尚未配置 API Key。请到「设置」填写 Base URL、模型与密钥。")
    class InvalidUrl : AiError("Base URL 无效。示例：https://api.deepseek.com/v1")
    class HttpStatus(code: Int, body: String) : AiError("服务返回 $code：${body.take(180)}")
    class EmptyContent : AiError("模型未返回有效文本，请重试或更换模型。")
    class Network(cause: Throwable) : AiError("网络错误：${cause.localizedMessage ?: cause.message}")
}

object AiPrompt {
    val SYSTEM_MESSAGE = """
你是紫微斗数学习助手。根据用户命盘结构作简明解读。
硬性要求：
1. 仅供参考，不保证吉凶，不作绝对预言或医疗/法律/投资承诺。
2. 结合命宫、财帛、官禄、夫妻等宫位主星与庙旺，具体分析。
3. 使用简体中文，按【性格格局】【事业财运】【感情人际】【提醒】四段输出。
""".trimIndent()

    fun cacheKey(chart: ZiWeiChart, model: String): String =
        listOf(chart.id.toString(), chart.ganzhiLine, chart.fiveElementBureau.displayName, model)
            .joinToString("|")

    fun userPayload(chart: ZiWeiChart): String {
        val lines = mutableListOf<String>()
        lines += "【命主】${chart.name.ifBlank { "未填" }} · ${chart.gender.displayName}"
        lines += "【四柱】${chart.ganzhiLine}"
        lines += "【农历】${chart.lunarLine}"
        lines += "【局】${chart.fiveElementBureau.displayName} 命宫${chart.mingBranch.hanzi} 身宫${chart.shenBranch.hanzi}"
        chart.cells.forEach { c ->
            val majors = c.majorStars.joinToString(",") { "${it.star.hanzi}${it.brightness.hanzi}" }
            val others = c.otherStars.joinToString(",") { it.star.hanzi }
            lines += "${c.type.hanzi}(${c.stem.hanzi}${c.branch.hanzi}): 主[$majors] 辅[$others]"
        }
        return lines.joinToString("\n")
    }
}

class AiClient {
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .build()

    suspend fun interpret(chart: ZiWeiChart, settings: AiSettings): String = withContext(Dispatchers.IO) {
        if (!settings.isConfigured) throw AiError.NotConfigured()
        val base = settings.resolvedBaseUrl.trimEnd('/')
        if (!base.startsWith("https://") && !base.startsWith("http://")) throw AiError.InvalidUrl()
        val url = "$base/chat/completions"
        val body = JSONObject()
            .put("model", settings.resolvedModel)
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", AiPrompt.SYSTEM_MESSAGE))
                    .put(JSONObject().put("role", "user").put("content", AiPrompt.userPayload(chart))),
            )
            .put("temperature", 0.6)
            .toString()
            .toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${settings.apiKey.trim()}")
            .post(body)
            .build()
        try {
            client.newCall(request).execute().use { resp ->
                val raw = resp.body?.string().orEmpty()
                if (!resp.isSuccessful) throw AiError.HttpStatus(resp.code, raw)
                val content = JSONObject(raw)
                    .optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    .orEmpty()
                    .trim()
                if (content.isEmpty()) throw AiError.EmptyContent()
                content
            }
        } catch (e: AiError) {
            throw e
        } catch (e: Exception) {
            throw AiError.Network(e)
        }
    }
}
