package com.gglee.xhotpost.domain

import okhttp3.OkHttpClient
import okhttp3.Request
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.TimeUnit

/**
 * Collects what's hot **on X** via a public trends mirror (getdaytrends),
 * not Google News. No official X API.
 */
class TrendCollector(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
) {
    private val nicheKeywords: Map<NicheId, List<String>> = mapOf(
        NicheId.TECH to listOf(
            "ai", "chatgpt", "gpt", "openai", "llm", "robot", "科技", "人工智能",
            "大模型", "芯片", "半导体", "apple", "google", "microsoft", "meta",
            "nvidia", "tesla", "iphone", "android", "crypto", "bitcoin", "web3",
            "startup", "saas", "编程", "开发", "开源", "github",
        ),
        NicheId.FINANCE to listOf(
            "stock", "market", "fed", "利率", "股市", "理财", "基金", "比特币",
            "crypto", "bitcoin", "eth", "nft", "nasdaq", "dow", "通胀", "gdp",
            "银行", "投资", "美联储", "汇率", "gold", "oil",
        ),
        NicheId.LIFESTYLE to listOf(
            "health", "fitness", "travel", "food", "咖啡", "旅行", "健康", "生活",
            "fashion", "beauty", "recipe", "workout", "diet", "wellness", "家居",
        ),
        NicheId.CREATOR to listOf(
            "creator", "influencer", "youtube", "tiktok", "自媒体", "副业",
            "content", "podcast", "直播", "带货", "newsletter", "订阅", "粉丝",
            "monetiz", "变现",
        ),
        NicheId.LOCAL to emptyList(),
        NicheId.CUSTOM to emptyList(),
    )

    private val demoTopics = listOf(
        HotTopic(
            id = topicId("#OpenSourceAI", "x-demo"),
            title = "#OpenSourceAI",
            summary = "X 热搜演示：开源模型话题正在讨论区发酵，适合发一条带判断的短帖。",
            source = "X 热搜 · demo",
            url = xSearchUrl("#OpenSourceAI"),
            score = 96,
            language = "en",
            fetchedAt = System.currentTimeMillis(),
        ),
        HotTopic(
            id = topicId("创作者经济", "x-demo"),
            title = "创作者经济",
            summary = "X 热搜演示：创作者在讨论如何把长内容拆成可复用短帖矩阵。",
            source = "X 热搜 · demo",
            url = xSearchUrl("创作者经济"),
            score = 88,
            language = "zh",
            fetchedAt = System.currentTimeMillis(),
        ),
        HotTopic(
            id = topicId("#BuildInPublic", "x-demo"),
            title = "#BuildInPublic",
            summary = "X 热搜演示：公开构建与审核发帖成为常见打法。",
            source = "X 热搜 · demo",
            url = xSearchUrl("#BuildInPublic"),
            score = 81,
            language = "en",
            fetchedAt = System.currentTimeMillis(),
        ),
    )

    fun collect(settings: AppSettings): List<HotTopic> {
        val now = System.currentTimeMillis()
        val collected = mutableListOf<HotTopic>()

        if (settings.demoMode) {
            collected += demoTopics.map { it.copy(fetchedAt = now) }
        }

        val regions = resolveRegions(settings)
        for (region in regions) {
            try {
                collected += fetchXTrends(region, now)
            } catch (_: Exception) {
                // try next region
            }
        }

        if (collected.isEmpty()) {
            collected += demoTopics.map {
                it.copy(
                    id = topicId(it.title, "fallback"),
                    fetchedAt = now,
                    source = "X 热搜 · fallback",
                )
            }
        }

        val ranked = rankForNiche(collected, settings)
        return ranked
            .groupBy { normalizeTitle(it.title) }
            .map { (_, items) -> items.maxBy { it.score } }
            .sortedByDescending { it.score }
            .take(40)
    }

    private fun resolveRegions(settings: AppSettings): List<String> {
        return when (settings.xTrendRegion) {
            XTrendRegion.UNITED_STATES -> listOf("united-states")
            XTrendRegion.UNITED_KINGDOM -> listOf("united-kingdom")
            XTrendRegion.JAPAN -> listOf("japan")
            XTrendRegion.SINGAPORE -> listOf("singapore")
            XTrendRegion.INDIA -> listOf("india")
            XTrendRegion.AUTO -> when (settings.language) {
                ContentLanguage.ZH -> listOf("japan", "singapore", "united-states")
                ContentLanguage.EN -> listOf("united-states", "united-kingdom")
                ContentLanguage.MIXED -> listOf("united-states", "japan", "singapore")
            }
        }
    }

    private fun fetchXTrends(regionSlug: String, now: Long): List<HotTopic> {
        val url = "https://getdaytrends.com/$regionSlug/"
        val request = Request.Builder()
            .url(url)
            .header(
                "User-Agent",
                "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36",
            )
            .header("Accept-Language", "en-US,en;q=0.9,zh-CN;q=0.8")
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("X trends ${response.code}")
            val body = response.body?.string() ?: error("empty trends")
            return parseGetDayTrends(body, regionSlug, now)
        }
    }

    /** Visible for unit tests. */
    fun parseGetDayTrends(html: String, regionSlug: String, now: Long): List<HotTopic> {
        val regionLabel = regionLabel(regionSlug)
        val pattern = Regex(
            """<td class="main"><a[^>]*>([^<]+)</a></td>""",
            RegexOption.IGNORE_CASE,
        )
        val names = pattern.findAll(html)
            .map { it.groupValues[1].trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .take(50)
            .toList()

        return names.mapIndexed { index, name ->
            val lang = guessLanguage(name)
            HotTopic(
                id = topicId(name, "x-$regionSlug"),
                title = name,
                summary = "X 热搜 · $regionLabel · 第 ${index + 1} 名。打开可看实时讨论，适合结合你的赛道写观点帖。",
                source = "X 热搜 · $regionLabel",
                url = xSearchUrl(name),
                score = (100 - index).coerceAtLeast(40),
                language = lang,
                fetchedAt = now,
            )
        }
    }

    private fun rankForNiche(topics: List<HotTopic>, settings: AppSettings): List<HotTopic> {
        val keywords = when (settings.niche) {
            NicheId.CUSTOM -> {
                val custom = settings.customNicheLabel.trim()
                if (custom.isBlank()) emptyList()
                else custom.split(Regex("[,，\\s]+")).map { it.trim().lowercase() }.filter { it.length >= 2 }
            }
            NicheId.LOCAL -> emptyList()
            else -> nicheKeywords[settings.niche].orEmpty()
        }

        if (keywords.isEmpty()) return topics

        val matched = mutableListOf<HotTopic>()
        val rest = mutableListOf<HotTopic>()
        for (topic in topics) {
            val hay = topic.title.lowercase()
            val hit = keywords.any { hay.contains(it) }
            if (hit) {
                matched += topic.copy(score = (topic.score + 25).coerceAtMost(120))
            } else {
                rest += topic
            }
        }

        // Prefer niche hits, then keep enough general X trends so the list never feels empty.
        return matched + rest.take(12)
    }

    private fun regionLabel(slug: String): String = when (slug) {
        "united-states" -> "美国"
        "united-kingdom" -> "英国"
        "japan" -> "日本"
        "singapore" -> "新加坡"
        "india" -> "印度"
        else -> slug
    }

    private fun guessLanguage(name: String): String {
        return if (name.any { it.code in 0x4E00..0x9FFF || it.code in 0x3040..0x30FF }) "zh" else "en"
    }

    private fun normalizeTitle(title: String): String =
        title.trim().lowercase().removePrefix("#")

    companion object {
        fun topicId(title: String, source: String): String {
            val digest = MessageDigest.getInstance("SHA-1")
                .digest("$source::$title".toByteArray())
            return digest.joinToString("") { "%02x".format(it) }.take(16)
        }

        fun xSearchUrl(query: String): String {
            val q = URLEncoder.encode(query, Charsets.UTF_8.name())
            return "https://x.com/search?q=$q&src=typed_query&f=live"
        }
    }
}
