package com.gglee.xhotpost.domain

enum class VideoPlatform {
    X,
    YOUTUBE,
    DOUYIN,
    UNKNOWN,
}

data class MediaLink(
    val platform: VideoPlatform,
    val id: String,
    val canonicalUrl: String,
    val raw: String,
    val username: String? = null,
) {
    val shortLabel: String
        get() = when (platform) {
            VideoPlatform.X ->
                if (!username.isNullOrBlank()) "@$username / $id" else "X · $id"
            VideoPlatform.YOUTUBE -> "YouTube · $id"
            VideoPlatform.DOUYIN -> "抖音 · $id"
            VideoPlatform.UNKNOWN -> canonicalUrl
        }
}

object MediaLinkParser {
    private val anyUrl = Regex(
        """https?://[^\s<>"']+""",
        RegexOption.IGNORE_CASE,
    )

    fun extract(raw: String): MediaLink? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        val url = anyUrl.find(text)?.value
            ?: text.takeIf { it.startsWith("http://") || it.startsWith("https://") }
            ?: return null

        XLinkParser.extract(url)?.takeIf { !it.statusId.isNullOrBlank() }?.let {
            return MediaLink(
                platform = VideoPlatform.X,
                id = it.statusId!!,
                canonicalUrl = it.canonicalUrl,
                raw = text,
                username = it.username,
            )
        }

        parseYouTube(url, text)?.let { return it }
        parseDouyin(url, text)?.let { return it }
        return null
    }

    private fun parseYouTube(url: String, raw: String): MediaLink? {
        val patterns = listOf(
            // watch?v=ID  or  watch?foo=1&v=ID  (the ? after watch is already consumed)
            Regex(
                """(?:youtube\.com|music\.youtube\.com)/watch\?[^\s#]*?\bv=([A-Za-z0-9_-]{6,})""",
                RegexOption.IGNORE_CASE,
            ),
            Regex(
                """(?:youtube\.com/(?:shorts|embed|live)/|youtu\.be/)([A-Za-z0-9_-]{6,})""",
                RegexOption.IGNORE_CASE,
            ),
        )
        for (p in patterns) {
            val m = p.find(url) ?: continue
            val id = m.groupValues[1]
            return MediaLink(
                platform = VideoPlatform.YOUTUBE,
                id = id,
                canonicalUrl = "https://www.youtube.com/watch?v=$id",
                raw = raw,
            )
        }
        return null
    }

    private fun parseDouyin(url: String, raw: String): MediaLink? {
        val hostOk = url.contains("douyin.com", ignoreCase = true) ||
            url.contains("iesdouyin.com", ignoreCase = true)
        if (!hostOk) return null

        val idMatch = Regex(
            """(?:video|note|share/video|aweme/detail)/(\d{15,25})""",
            RegexOption.IGNORE_CASE,
        ).find(url)
        if (idMatch != null) {
            val id = idMatch.groupValues[1]
            return MediaLink(
                platform = VideoPlatform.DOUYIN,
                id = id,
                canonicalUrl = "https://www.douyin.com/video/$id",
                raw = raw,
            )
        }
        // Short links like v.douyin.com/xxxx — id resolved later by following redirects.
        if (url.contains("v.douyin.com", ignoreCase = true)) {
            val slug = url.trimEnd('/').substringAfterLast('/')
            if (slug.isNotBlank()) {
                return MediaLink(
                    platform = VideoPlatform.DOUYIN,
                    id = "short:$slug",
                    canonicalUrl = url.substringBefore('?').trimEnd('/'),
                    raw = raw,
                )
            }
        }
        return MediaLink(
            platform = VideoPlatform.DOUYIN,
            id = "raw",
            canonicalUrl = url.substringBefore('?'),
            raw = raw,
        )
    }
}
