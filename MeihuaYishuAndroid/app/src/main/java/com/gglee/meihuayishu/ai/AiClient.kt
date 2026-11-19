package com.gglee.meihuayishu.ai

import com.gglee.meihuayishu.engine.MeihuaChart
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
    class InvalidUrl : AiError("Base URL 无效。示例：https://api.deepseek.com/v1 或 https://api.openai.com/v1")
    class HttpStatus(val code: Int, body: String) :
        AiError("服务返回 $code：${body.take(180)}")

    class EmptyContent : AiError("模型未返回有效文本，请重试或更换模型。")
    class Network(cause: Throwable) : AiError("网络错误：${cause.localizedMessage ?: cause.message}。可导出图片给其他 AI。")
}

object AiPrompt {
    val SYSTEM_MESSAGE = """
你是梅花易数学习助手。根据用户提供的「所问之事」与本卦/互卦/变卦/体用结构作针对性解读。
硬性要求：
1. 仅供参考，不保证吉凶，不作绝对预言或医疗/法律/投资承诺。
2. 必须针对所问之事，结合体用生克、动爻与互变，具体分析，避免空话套话。
3. 使用简体中文，按以下四段输出（用小标题）：
   【总断】【有利因素】【风险】【行动建议】
4. 若所问为空，先说明应补问，再给极简盘面纲要。
""".trimIndent()

    fun cacheKey(chart: MeihuaChart, model: String): String =
        listOf(
            chart.id.toString(),
            chart.question,
            chart.method.name,
            chart.original.title,
            chart.mutual.title,
            chart.changed.title,
            chart.ti.hanzi,
            chart.yong.hanzi,
            chart.movingLine.toString(),
            model,
        ).joinToString("|")

    fun userPayload(chart: MeihuaChart): String {
        val lines = mutableListOf<String>()
        lines += "【所问之事】${if (chart.hasQuestion) chart.question else "（未填写）"}"
        lines += "【起卦法】${chart.method.label} — ${chart.methodDetail}"
        if (chart.ganzhiLine.isNotBlank()) lines += "【干支】${chart.ganzhiLine}"
        lines += "【本卦】${chart.original.title}（上${chart.original.upper.hanzi}${chart.original.upper.symbol} 下${chart.original.lower.hanzi}${chart.original.lower.symbol}）"
        lines += "【动爻】第${chart.movingLine}爻"
        lines += "【互卦】${chart.mutual.title}"
        lines += "【变卦】${chart.changed.title}"
        lines += "【体卦】${chart.ti.displayName}（${chart.ti.element.hanzi}）"
        lines += "【用卦】${chart.yong.displayName}（${chart.yong.element.hanzi}）"
        lines += "【体用】${chart.tiYongRelation.label} — ${chart.tiYongRelation.hint}"
        lines += "【六爻】（自下而上，●阳 ○阴，*动）"
        chart.original.lines.forEachIndexed { i, yang ->
            val mark = if (yang) "●" else "○"
            val moving = if (i + 1 == chart.movingLine) " *" else ""
            lines += "第${i + 1}爻 $mark$moving"
        }
        val q = if (chart.hasQuestion) chart.question else "未填问题"
        lines += "请严格按系统要求的四段结构，针对「$q」给出明确解读。"
        return lines.joinToString("\n")
    }

    /** 适合粘贴到 ChatGPT / Claude 等的完整提示词 */
    fun exportPromptForExternalAi(chart: MeihuaChart): String =
        SYSTEM_MESSAGE + "\n\n" + userPayload(chart)
}

class AiClient(
    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build(),
) {
    data class Config(
        val baseUrl: String,
        val apiKey: String,
        val model: String,
    )

    fun normalizedChatUrl(baseUrl: String): String? {
        var b = baseUrl.trim()
        if (b.isEmpty()) return null
        while (b.endsWith("/")) b = b.dropLast(1)
        if (!b.lowercase().endsWith("/v1")) {
            b += "/v1"
        }
        return "$b/chat/completions"
    }

    suspend fun interpret(chart: MeihuaChart, config: Config): String = withContext(Dispatchers.IO) {
        val key = config.apiKey.trim()
        if (key.isEmpty()) throw AiError.NotConfigured()
        val url = normalizedChatUrl(config.baseUrl)
            ?: throw AiError.InvalidUrl()

        val messages = JSONArray().apply {
            put(JSONObject().put("role", "system").put("content", AiPrompt.SYSTEM_MESSAGE))
            put(JSONObject().put("role", "user").put("content", AiPrompt.userPayload(chart)))
        }
        val bodyJson = JSONObject()
            .put("model", config.model)
            .put("temperature", 0.4)
            .put("messages", messages)
        val body = bodyJson.toString().toRequestBody("application/json".toMediaType())

        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Content-Type", "application/json")
            .header("Authorization", "Bearer $key")
            .build()

        try {
            http.newCall(request).execute().use { response ->
                val text = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    throw AiError.HttpStatus(response.code, text)
                }
                val root = JSONObject(text)
                val choices = root.optJSONArray("choices")
                val first = choices?.optJSONObject(0)
                val message = first?.optJSONObject("message")
                val content = message?.optString("content")?.trim()
                if (content.isNullOrEmpty()) throw AiError.EmptyContent()
                content
            }
        } catch (e: AiError) {
            throw e
        } catch (e: Exception) {
            throw AiError.Network(e)
        }
    }
}
