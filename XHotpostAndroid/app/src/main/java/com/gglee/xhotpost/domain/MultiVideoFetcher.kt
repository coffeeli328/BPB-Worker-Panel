package com.gglee.xhotpost.domain

import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.URLDecoder
import java.util.concurrent.TimeUnit

data class RemoteVideo(
    val link: MediaLink,
    val title: String,
    val author: String?,
    val mp4Url: String,
    val width: Int? = null,
    val height: Int? = null,
    val durationSec: Double? = null,
)

/**
 * Resolve + download public MP4s from X / YouTube / Douyin (no official APIs).
 * Douyin may fail on some networks; UI should offer local file pick as fallback.
 */
class MultiVideoFetcher(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(90, TimeUnit.SECONDS)
        .followRedirects(true)
        .build(),
    private val xFetcher: XVideoFetcher = XVideoFetcher(client),
) {
    fun resolve(rawInput: String): RemoteVideo {
        val link = MediaLinkParser.extract(rawInput)
            ?: error("请粘贴 X / YouTube / 抖音 视频链接")
        return when (link.platform) {
            VideoPlatform.X -> resolveX(link)
            VideoPlatform.YOUTUBE -> resolveYouTube(link)
            VideoPlatform.DOUYIN -> resolveDouyin(link)
            VideoPlatform.UNKNOWN -> error("暂不支持该链接平台")
        }
    }

    fun download(info: RemoteVideo, dest: File): File {
        dest.parentFile?.mkdirs()
        if (dest.exists()) dest.delete()
        val referer = when (info.link.platform) {
            VideoPlatform.X -> "https://x.com/"
            VideoPlatform.YOUTUBE -> "https://www.youtube.com/"
            VideoPlatform.DOUYIN -> "https://www.douyin.com/"
            else -> info.link.canonicalUrl
        }
        val request = Request.Builder()
            .url(info.mp4Url)
            .header("User-Agent", USER_AGENT)
            .header("Referer", referer)
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) error("下载失败 HTTP ${response.code}")
            val body = response.body ?: error("下载为空")
            dest.outputStream().use { out ->
                body.byteStream().use { input -> input.copyTo(out) }
            }
        }
        if (!dest.exists() || dest.length() < 1024L) {
            dest.delete()
            error("下载的视频文件无效")
        }
        // X 通常限制较大视频；超过 ~500MB 提醒（仍允许）
        return dest
    }

    private fun resolveX(link: MediaLink): RemoteVideo {
        val parsed = ParsedXLink(
            canonicalUrl = link.canonicalUrl,
            username = link.username,
            statusId = link.id,
            raw = link.raw,
        )
        val info = xFetcher.resolve(parsed)
        return RemoteVideo(
            link = link.copy(username = info.author ?: link.username),
            title = info.tweetText.ifBlank { "X 视频" },
            author = info.author,
            mp4Url = info.mp4Url,
            width = info.width,
            height = info.height,
            durationSec = info.durationSec,
        )
    }

    private fun resolveYouTube(link: MediaLink): RemoteVideo {
        val body = JSONObject()
            .put(
                "context",
                JSONObject().put(
                    "client",
                    JSONObject()
                        .put("clientName", "ANDROID")
                        .put("clientVersion", "20.10.38")
                        .put("androidSdkVersion", 34)
                        .put("hl", "zh-CN")
                        .put("gl", "US")
                        .put("utcOffsetMinutes", 0),
                ),
            )
            .put("videoId", link.id)
            .put("contentCheckOk", true)
            .put("racyCheckOk", true)

        val request = Request.Builder()
            .url("https://www.youtube.com/youtubei/v1/player?prettyPrint=false")
            .header("Content-Type", "application/json")
            .header("User-Agent", "com.google.android.youtube/20.10.38 (Linux; U; Android 14) gzip")
            .header("X-YouTube-Client-Name", "3")
            .header("X-YouTube-Client-Version", "20.10.38")
            .post(body.toString().toRequestBody(JSON))
            .build()

        client.newCall(request).execute().use { response ->
            val raw = response.body?.string().orEmpty()
            if (!response.isSuccessful) error("YouTube 解析失败 HTTP ${response.code}")
            val root = JSONObject(raw)
            val status = root.optJSONObject("playabilityStatus")?.optString("status")
            if (status != null && status != "OK") {
                val reason = root.optJSONObject("playabilityStatus")?.optString("reason")
                error("YouTube 无法播放：${reason ?: status}")
            }
            val details = root.optJSONObject("videoDetails")
            val title = details?.optString("title").orEmpty().ifBlank { "YouTube 视频" }
            val author = details?.optString("author")
            val duration = details?.optString("lengthSeconds")?.toDoubleOrNull()
            val streaming = root.optJSONObject("streamingData")
                ?: error("YouTube 未返回流地址")
            val mp4 = pickYouTubeMp4(streaming) ?: error("未找到可下载的 MP4（可能是直播/会员内容）")
            return RemoteVideo(
                link = link,
                title = title,
                author = author,
                mp4Url = mp4.first,
                width = mp4.second,
                height = mp4.third,
                durationSec = duration,
            )
        }
    }

    private fun pickYouTubeMp4(streaming: JSONObject): Triple<String, Int?, Int?>? {
        // Prefer progressive formats (audio+video), e.g. itag 18 = 360p.
        val progressive = streaming.optJSONArray("formats") ?: JSONArray()
        var best: Triple<String, Int, Pair<Int?, Int?>>? = null
        for (i in 0 until progressive.length()) {
            val f = progressive.optJSONObject(i) ?: continue
            val mime = f.optString("mimeType")
            val url = f.optString("url")
            if (!mime.contains("mp4", ignoreCase = true) || url.isBlank()) continue
            if (!mime.contains("avc1", ignoreCase = true) && !mime.contains("mp4a", ignoreCase = true)) {
                // still accept generic mp4
            }
            val height = Regex("""(\d+)p""").find(f.optString("qualityLabel"))
                ?.groupValues?.getOrNull(1)?.toIntOrNull()
                ?: f.optInt("height").takeIf { it > 0 }
            val width = f.optInt("width").takeIf { it > 0 }
            val score = when (height) {
                in 360..720 -> 1000 + (height ?: 0)
                in 240..359 -> 500 + (height ?: 0)
                else -> (height ?: 0)
            }
            if (best == null || score > best.second) {
                best = Triple(url, score, width to height)
            }
        }
        best?.let { return Triple(it.first, it.third.first, it.third.second) }

        // Fallback: adaptive video-only mp4 (no audio) — better than nothing.
        val adaptive = streaming.optJSONArray("adaptiveFormats") ?: JSONArray()
        for (i in 0 until adaptive.length()) {
            val f = adaptive.optJSONObject(i) ?: continue
            val mime = f.optString("mimeType")
            val url = f.optString("url")
            if (mime.startsWith("video/mp4") && url.isNotBlank()) {
                return Triple(
                    url,
                    f.optInt("width").takeIf { it > 0 },
                    f.optInt("height").takeIf { it > 0 },
                )
            }
        }
        return null
    }

    private fun resolveDouyin(link: MediaLink): RemoteVideo {
        var working = link
        if (link.id.startsWith("short:") || link.id == "raw") {
            working = expandDouyin(link)
        }
        val errors = mutableListOf<String>()

        // 1) tikwm
        try {
            return resolveDouyinTikwm(working)
        } catch (e: Exception) {
            errors += "tikwm: ${e.message?.take(80)}"
        }

        // 2) iesdouyin share page / iteminfo patterns
        try {
            return resolveDouyinSharePage(working)
        } catch (e: Exception) {
            errors += "share: ${e.message?.take(80)}"
        }

        error(
            "抖音解析失败（${errors.joinToString("；").ifBlank { "网络限制" }}）。" +
                "可点「选择本地视频」先下载后再发。",
        )
    }

    private fun expandDouyin(link: MediaLink): MediaLink {
        val request = Request.Builder()
            .url(link.canonicalUrl)
            .header("User-Agent", MOBILE_UA)
            .get()
            .build()
        // Use a no-follow client for Location, but OkHttp follows by default — final URL is enough.
        client.newCall(request).execute().use { response ->
            val finalUrl = response.request.url.toString()
            val parsed = MediaLinkParser.extract(finalUrl)
            if (parsed?.platform == VideoPlatform.DOUYIN &&
                !parsed.id.startsWith("short:") &&
                parsed.id != "raw" &&
                parsed.id.all { it.isDigit() }
            ) {
                return parsed
            }
            val id = Regex("""(?:video|note|share/video)/(\d{15,25})""")
                .find(finalUrl)?.groupValues?.getOrNull(1)
            if (id != null) {
                return MediaLink(
                    platform = VideoPlatform.DOUYIN,
                    id = id,
                    canonicalUrl = "https://www.douyin.com/video/$id",
                    raw = link.raw,
                )
            }
            // Keep original; later steps may still work with short URL via tikwm.
            return link
        }
    }

    private fun resolveDouyinTikwm(link: MediaLink): RemoteVideo {
        val target = if (link.id.all { it.isDigit() }) {
            "https://www.douyin.com/video/${link.id}"
        } else {
            link.canonicalUrl
        }
        val url = "https://www.tikwm.com/api/?url=${java.net.URLEncoder.encode(target, "UTF-8")}"
        val raw = httpGet(url, MOBILE_UA) ?: error("tikwm 无响应")
        val root = JSONObject(raw)
        if (root.optInt("code", -1) != 0) {
            error(root.optString("msg").ifBlank { "tikwm 解析失败" })
        }
        val data = root.optJSONObject("data") ?: error("tikwm 无 data")
        val play = sequenceOf("play", "wmplay", "hdplay")
            .map { data.optString(it) }
            .firstOrNull { it.isNotBlank() }
            ?: error("tikwm 未返回播放地址")
        return RemoteVideo(
            link = link,
            title = data.optString("title").ifBlank { "抖音视频" },
            author = data.optJSONObject("author")?.optString("unique_id")
                ?: data.optJSONObject("author")?.optString("nickname"),
            mp4Url = play,
            durationSec = data.optDouble("duration").takeIf { it > 0 },
        )
    }

    private fun resolveDouyinSharePage(link: MediaLink): RemoteVideo {
        if (!link.id.all { it.isDigit() }) error("缺少抖音视频 ID")
        val pageUrl = "https://www.iesdouyin.com/share/video/${link.id}/"
        val html = httpGet(pageUrl, MOBILE_UA) ?: error("分享页无响应")
        val render = Regex("""id="RENDER_DATA"[^>]*content="([^"]+)"""")
            .find(html)?.groupValues?.getOrNull(1)
            ?.let { URLDecoder.decode(it, "UTF-8") }
        val blob = render ?: html
        val mp4 = Regex("""https://[^"'\s\\]+?\.mp4[^"'\s\\]*""")
            .findAll(blob)
            .map { it.value.replace("\\u002F", "/").replace("\\u0026", "&") }
            .firstOrNull()
            ?: Regex(""""(?:play_addr|playAddr)"\s*:\s*\{[^}]*?"url_list"\s*:\s*\[\s*"([^"]+)"""")
                .find(blob)?.groupValues?.getOrNull(1)
                ?.replace("\\u002F", "/")
                ?.replace("\\u0026", "&")
            ?: error("分享页未找到 MP4")
        val title = Regex(""""desc"\s*:\s*"((?:\\.|[^"\\])*)"""")
            .find(blob)?.groupValues?.getOrNull(1)
            ?.replace("\\n", " ")
            ?.replace("\\\"", "\"")
            .orEmpty()
            .ifBlank { "抖音视频" }
        return RemoteVideo(
            link = link,
            title = title,
            author = null,
            mp4Url = mp4.replace("playwm", "play"),
        )
    }

    private fun httpGet(url: String, ua: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", ua)
            .header("Accept", "application/json,text/html,*/*")
            .build()
        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return null
            return response.body?.string()
        }
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/120.0.0.0 Mobile Safari/537.36"
        private const val MOBILE_UA =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 16_6 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1"
    }
}
