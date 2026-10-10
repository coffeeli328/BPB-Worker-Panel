package com.gglee.xhotpost.domain

import okhttp3.OkHttpClient
import okhttp3.Request
import org.xmlpull.v1.XmlPullParser
import org.xmlpull.v1.XmlPullParserFactory
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

class TrendCollector(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build(),
) {
    private val nicheQueries: Map<NicheId, Pair<String, String>> = mapOf(
        NicheId.TECH to ("人工智能 OR ChatGPT OR 大模型" to "artificial intelligence OR ChatGPT OR LLM"),
        NicheId.FINANCE to ("股市 OR 理财 OR 加密货币" to "markets OR investing OR crypto"),
        NicheId.LIFESTYLE to ("健康 OR 旅行 OR 生活" to "wellness OR travel OR lifestyle"),
        NicheId.CREATOR to ("自媒体 OR 内容创作 OR 副业" to "creator economy OR content creator"),
        NicheId.LOCAL to ("热点新闻" to "breaking news"),
        NicheId.CUSTOM to ("热点" to "trending"),
    )

    private val demoTopics = listOf(
        HotTopic(
            id = topicId("开源模型掀起新一轮 AI 应用潮", "demo"),
            title = "开源模型掀起新一轮 AI 应用潮",
            summary = "开发者用开源模型快速搭产品，成本下降，竞争转向落地场景。",
            source = "demo",
            url = null,
            score = 96,
            language = "zh",
            fetchedAt = System.currentTimeMillis(),
        ),
        HotTopic(
            id = topicId("创作者开始把长内容拆成可复用短帖矩阵", "demo"),
            title = "创作者开始把长内容拆成可复用短帖矩阵",
            summary = "同一素材多平台分发，审核后定时发布，成为常见变现打法。",
            source = "demo",
            url = null,
            score = 88,
            language = "zh",
            fetchedAt = System.currentTimeMillis(),
        ),
        HotTopic(
            id = topicId("小额付费社群回暖：信任比流量更值钱", "demo"),
            title = "小额付费社群回暖：信任比流量更值钱",
            summary = "高互动小群比大粉更稳定，热点解读 + 会员权益成为组合拳。",
            source = "demo",
            url = null,
            score = 81,
            language = "zh",
            fetchedAt = System.currentTimeMillis(),
        ),
    )

    suspend fun collect(settings: AppSettings): List<HotTopic> {
        val now = System.currentTimeMillis()
        val collected = mutableListOf<HotTopic>()

        if (settings.demoMode) {
            collected += demoTopics.map { it.copy(fetchedAt = now) }
        }

        val queries = if (settings.niche == NicheId.CUSTOM && settings.customNicheLabel.isNotBlank()) {
            settings.customNicheLabel to settings.customNicheLabel
        } else {
            nicheQueries[settings.niche] ?: nicheQueries[NicheId.TECH]!!
        }

        try {
            if (settings.language != ContentLanguage.EN) {
                collected += fetchRss(queries.first, "zh", now)
            }
            if (settings.language != ContentLanguage.ZH) {
                collected += fetchRss(queries.second, "en", now)
            }
        } catch (_: Exception) {
            if (collected.isEmpty()) {
                collected += demoTopics.map { it.copy(id = topicId(it.title, "fallback"), fetchedAt = now) }
            }
        }

        return collected
            .groupBy { it.id }
            .map { (_, items) -> items.maxBy { it.score } }
            .sortedByDescending { it.score }
    }

    private fun fetchRss(query: String, lang: String, now: Long): List<HotTopic> {
        val url = if (lang == "zh") {
            val q = URLEncoder.encode(query, Charsets.UTF_8.name())
            "https://news.google.com/rss/search?q=$q&hl=zh-CN&gl=CN&ceid=CN:zh-Hans"
        } else {
            val q = URLEncoder.encode(query, Charsets.UTF_8.name())
            "https://news.google.com/rss/search?q=$q&hl=en-US&gl=US&ceid=US:en"
        }

        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "XHotPostAndroid/1.0")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("RSS ${response.code}")
            val body = response.body?.string() ?: error("empty rss")
            return parseRss(body, lang, now)
        }
    }

    private fun parseRss(xml: String, lang: String, now: Long): List<HotTopic> {
        val factory = XmlPullParserFactory.newInstance()
        val parser = factory.newPullParser()
        parser.setInput(xml.reader())

        val items = mutableListOf<HotTopic>()
        var event = parser.eventType
        var inItem = false
        var title = ""
        var link: String? = null
        var summary = ""
        var index = 0

        while (event != XmlPullParser.END_DOCUMENT && items.size < 12) {
            when (event) {
                XmlPullParser.START_TAG -> when (parser.name) {
                    "item" -> {
                        inItem = true
                        title = ""
                        link = null
                        summary = ""
                    }
                    "title" -> if (inItem) title = parser.nextText().trim()
                    "link" -> if (inItem) link = parser.nextText().trim()
                    "description", "content" -> if (inItem && summary.isBlank()) {
                        summary = stripHtml(parser.nextText()).take(220)
                    }
                }
                XmlPullParser.END_TAG -> if (parser.name == "item" && inItem) {
                    if (title.isNotBlank()) {
                        items += HotTopic(
                            id = topicId(title, lang),
                            title = title,
                            summary = summary.ifBlank { title },
                            source = "Google News",
                            url = link,
                            score = (100 - index * 4).coerceAtLeast(40),
                            language = lang,
                            fetchedAt = now,
                        )
                        index++
                    }
                    inItem = false
                }
            }
            event = parser.next()
        }
        return items
    }

    private fun stripHtml(raw: String): String =
        raw.replace(Regex("<[^>]+>"), " ").replace(Regex("\\s+"), " ").trim()

    companion object {
        fun topicId(title: String, source: String): String {
            val digest = MessageDigest.getInstance("SHA-1")
                .digest("$source::$title".toByteArray())
            return digest.joinToString("") { "%02x".format(it) }.take(16)
        }
    }
}
