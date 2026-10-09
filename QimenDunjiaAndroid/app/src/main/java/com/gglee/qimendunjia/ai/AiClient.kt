package com.gglee.qimendunjia.ai

import com.gglee.qimendunjia.engine.EightDeity
import com.gglee.qimendunjia.engine.Palace
import com.gglee.qimendunjia.engine.QimenChart
import com.gglee.qimendunjia.engine.YongShenMapping
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
    class Network(cause: Throwable) : AiError("网络错误：${cause.localizedMessage ?: cause.message}。可继续使用本机规则解读。")
}

object AiPrompt {
    val SYSTEM_MESSAGE = """
你是时家奇门遁甲学习助手。根据用户提供的「所问之事」与结构化盘面作针对性解读。
硬性要求：
1. 仅供参考，不保证吉凶，不作绝对预言或医疗/法律/投资承诺。
2. 必须针对所问之事，结合用神相关宫与值符值使，具体分析，避免空话套话。
3. 使用简体中文，按以下四段输出（用小标题）：
   【总断】【有利因素】【风险】【行动建议】
4. 若所问为空，先说明应补问，再给极简盘面纲要。
""".trimIndent()

    fun cacheKey(chart: QimenChart, model: String): String {
        val parts = mutableListOf(
            chart.id.toString(),
            chart.question,
            chart.juTitle,
            chart.ganzhiLine,
            chart.zhiFuStar.hanzi,
            chart.zhiFuPalace.hanzi,
            chart.zhiShiGate.displayName,
            chart.zhiShiPalace.hanzi,
            chart.xunKong.joinToString("") { it.hanzi },
            model,
        )
        chart.cells.sortedBy { it.palace.rawValue }.forEach { c ->
            parts.add(
                listOf(
                    "${c.palace.rawValue}",
                    c.heavenStem?.hanzi ?: "",
                    c.earthStem?.hanzi ?: "",
                    c.star?.shortName ?: "",
                    c.gate?.displayName ?: "",
                    c.deity?.shortName ?: "",
                    if (c.isEmpty) "空" else "",
                    if (c.isZhiFu) "符" else "",
                    if (c.isZhiShi) "使" else "",
                ).joinToString(","),
            )
        }
        return parts.joinToString("|")
    }

    fun userPayload(chart: QimenChart): String {
        val focus = YongShenMapping.focus(chart.questionTopic, chart.cells)
        val focusNames = focus.palaces.joinToString("、") { "${it.hanzi}${it.rawValue}" }
        val gateHint = focus.preferredGates.joinToString("、") { it.displayName }
        val starHint = focus.preferredStars.joinToString("、") { it.hanzi }

        val lines = mutableListOf<String>()
        lines += "【所问之事】${if (chart.hasQuestion) chart.question else "（未填写）"}"
        lines += "【事项归类】${chart.questionTopic.rawValue} — ${chart.questionTopic.focusHint}"
        lines += "【用神提示宫】${if (focusNames.isEmpty()) "值符/值使" else focusNames}"
        if (gateHint.isNotEmpty()) lines += "【事门侧重】$gateHint"
        if (starHint.isNotEmpty()) lines += "【事星侧重】$starHint"
        lines += "【定局】${chart.juTitle} · ${chart.solarTermName} · ${chart.juPhase.rawValue}"
        lines += "【干支】${chart.ganzhiLine}"
        lines += "【值符】${chart.zhiFuStar.hanzi}在${chart.zhiFuPalace.hanzi}宫"
        lines += "【值使】${chart.zhiShiGate.displayName}在${chart.zhiShiPalace.hanzi}宫"
        lines += "【旬空】${chart.xunKong.joinToString("") { it.hanzi }}"
        lines += "【九宫】（宫|天盘干|地盘干|星|门|神|空亡|值符|值使）"
        chart.cells.sortedBy { it.palace.rawValue }.forEach { c ->
            val marks = buildList {
                if (c.isEmpty) add("空")
                if (c.isZhiFu) add("值符")
                if (c.isZhiShi) add("值使")
            }.joinToString("/")
            val deityName = c.deity?.let { deityName(it, chart.isYangDun) } ?: "·"
            lines += listOf(
                "${c.palace.hanzi}${c.palace.rawValue}",
                c.heavenStem?.hanzi ?: "·",
                c.earthStem?.hanzi ?: "·",
                c.star?.hanzi ?: "·",
                c.gate?.displayName ?: "·",
                deityName,
                if (marks.isEmpty()) "-" else marks,
            ).joinToString("|")
        }
        val q = if (chart.hasQuestion) chart.question else "未填问题"
        lines += "请严格按系统要求的四段结构，针对「$q」给出明确解读。"
        return lines.joinToString("\n")
    }

    private fun deityName(deity: EightDeity, isYangDun: Boolean): String = deity.name(isYangDun)
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

    suspend fun interpret(chart: QimenChart, config: Config): String = withContext(Dispatchers.IO) {
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
