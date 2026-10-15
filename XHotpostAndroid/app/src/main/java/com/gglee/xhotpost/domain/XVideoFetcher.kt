package com.gglee.xhotpost.domain

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class XVideoInfo(
    val parsed: ParsedXLink,
    val tweetText: String,
    val author: String?,
    val mp4Url: String,
    val width: Int?,
    val height: Int?,
    val durationSec: Double?,
)

/**
 * Resolves a public MP4 for an X status via FixTweet/FxTwitter JSON
 * (no official X API), then downloads it locally for sharing.
 */
class XVideoFetcher(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
) {
    fun resolve(parsed: ParsedXLink): XVideoInfo {
        val user = parsed.username ?: "i"
        val id = parsed.statusId ?: error("缺少帖子 ID")
        val endpoints = listOf(
            "https://api.fxtwitter.com/$user/status/$id",
            "https://api.fxtwitter.com/status/$id",
            "https://api.vxtwitter.com/$user/status/$id",
        )
        var lastError = "无法解析视频"
        for (endpoint in endpoints) {
            try {
                val json = httpGet(endpoint) ?: continue
                val info = parseFxTwitter(json, parsed)
                if (info != null) return info
                lastError = "该帖没有可下载的视频（可能是图片/仅文字/权限限制）"
            } catch (e: Exception) {
                lastError = e.message?.take(120) ?: "解析失败"
            }
        }
        error(lastError)
    }

    fun download(info: XVideoInfo, dest: File): File {
        dest.parentFile?.mkdirs()
        if (dest.exists()) dest.delete()
        val request = Request.Builder()
            .url(info.mp4Url)
            .header("User-Agent", USER_AGENT)
            .header("Referer", "https://x.com/")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("下载视频失败 HTTP ${response.code}")
            }
            val body = response.body ?: error("下载为空")
            dest.outputStream().use { out ->
                body.byteStream().use { input -> input.copyTo(out) }
            }
        }
        if (!dest.exists() || dest.length() < 1024) {
            dest.delete()
            error("下载的视频文件无效")
        }
        return dest
    }

    private fun httpGet(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "application/json")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()
        }
    }

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"

        private val mp4UrlRegex = Regex(
            """https://video\.twimg\.com/[^"'\\\s>]+\.mp4[^"'\\\s>]*""",
        )
        private val bitrateNearUrl = Regex(
            """"bitrate"\s*:\s*(\d+)[^}]{0,240}?"url"\s*:\s*"(https://video\.twimg\.com/[^"]+\.mp4[^"]*)"|"url"\s*:\s*"(https://video\.twimg\.com/[^"]+\.mp4[^"]*)"[^}]{0,240}?"bitrate"\s*:\s*(\d+)""",
        )

        /** Prefer ~720p when available (unit-testable, no Android JSON stubs). */
        fun pickBestMp4Url(rawJson: String): String? {
            val scored = mutableListOf<Pair<Int, String>>()
            for (m in bitrateNearUrl.findAll(rawJson)) {
                val br = m.groupValues[1].ifBlank { m.groupValues[4] }.toIntOrNull() ?: 0
                val url = m.groupValues[2].ifBlank { m.groupValues[3] }
                    .replace("\\u0026", "&")
                if (url.isNotBlank()) {
                    // Prefer ~0.8–3 Mbps (usually 360p–720p): small enough to share, clear enough to watch.
                    val score = when {
                        br in 800_000..3_000_000 -> br + 20_000_000
                        br in 3_000_001..6_000_000 -> br + 10_000_000
                        br > 0 -> br
                        else -> 1
                    }
                    scored += score to url
                }
            }
            if (scored.isNotEmpty()) {
                return scored.maxBy { it.first }.second
            }
            return mp4UrlRegex.findAll(rawJson)
                .map { it.value.replace("\\u0026", "&") }
                .firstOrNull()
        }

        fun parseFxTwitter(raw: String, parsed: ParsedXLink): XVideoInfo? {
            val mp4 = pickBestMp4Url(raw) ?: return null
            // Avoid photos-only payloads that somehow include no mp4 (already handled).
            if (!raw.contains("video.twimg.com")) return null

            val author = Regex(""""screen_name"\s*:\s*"([^"]+)"""")
                .find(raw)?.groupValues?.getOrNull(1)
                ?: parsed.username
            val text = Regex(""""text"\s*:\s*"((?:\\.|[^"\\])*)"""")
                .find(raw)?.groupValues?.getOrNull(1)
                ?.replace("\\n", "\n")
                ?.replace("\\\"", "\"")
                .orEmpty()
            val duration = Regex(""""duration"\s*:\s*([0-9.]+)""")
                .find(raw)?.groupValues?.getOrNull(1)?.toDoubleOrNull()
            val width = Regex(""""width"\s*:\s*(\d+)""")
                .find(raw)?.groupValues?.getOrNull(1)?.toIntOrNull()
            val height = Regex(""""height"\s*:\s*(\d+)""")
                .find(raw)?.groupValues?.getOrNull(1)?.toIntOrNull()

            // Prefer JSONObject details when the Android runtime provides a real implementation.
            try {
                val root = JSONObject(raw)
                val tweet = root.optJSONObject("tweet") ?: root
                val media = tweet.optJSONObject("media")
                if (media != null) {
                    val videos = media.optJSONArray("videos")
                    if (videos != null && videos.length() == 0 && !raw.contains(".mp4")) {
                        return null
                    }
                }
                val jsonAuthor = tweet.optJSONObject("author")?.optString("screen_name")
                val jsonText = tweet.optString("text").ifBlank { tweet.optString("full_text") }
                return XVideoInfo(
                    parsed = parsed,
                    tweetText = jsonText.ifBlank { text },
                    author = jsonAuthor?.takeIf { it.isNotBlank() } ?: author,
                    mp4Url = mp4,
                    width = width,
                    height = height,
                    durationSec = duration,
                )
            } catch (_: Throwable) {
                return XVideoInfo(
                    parsed = parsed,
                    tweetText = text,
                    author = author,
                    mp4Url = mp4,
                    width = width,
                    height = height,
                    durationSec = duration,
                )
            }
        }
    }
}
