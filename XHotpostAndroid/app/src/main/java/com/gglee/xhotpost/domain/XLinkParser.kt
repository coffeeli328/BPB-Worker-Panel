package com.gglee.xhotpost.domain

import java.net.URI
import java.util.regex.Pattern

data class ParsedXLink(
    val canonicalUrl: String,
    val username: String?,
    val statusId: String?,
    val raw: String,
) {
    val shortLabel: String
        get() = when {
            !username.isNullOrBlank() && !statusId.isNullOrBlank() ->
                "@$username / $statusId"
            !statusId.isNullOrBlank() -> "帖子 $statusId"
            else -> canonicalUrl
        }
}

object XLinkParser {
    private val urlInText = Pattern.compile(
        """https?://(?:www\.)?(?:x\.com|twitter\.com|mobile\.twitter\.com|mobile\.x\.com)/[^\s<>"']+""",
        Pattern.CASE_INSENSITIVE,
    )

    private val statusPath = Pattern.compile(
        """^/([A-Za-z0-9_]{1,15})/status/(\d+)""",
        Pattern.CASE_INSENSITIVE,
    )

    /** Pull the first X/Twitter status URL from clipboard / share text. */
    fun extract(raw: String): ParsedXLink? {
        val text = raw.trim()
        if (text.isEmpty()) return null

        val matcher = urlInText.matcher(text)
        val candidate = if (matcher.find()) {
            matcher.group()
        } else if (text.startsWith("http://") || text.startsWith("https://")) {
            text.split(Regex("\\s+")).first()
        } else {
            return null
        }

        return parseUrl(candidate)?.copy(raw = text)
    }

    fun parseUrl(url: String): ParsedXLink? {
        val cleaned = url.trim()
            .removeSuffix("/")
            .substringBefore("?")
            .substringBefore("#")
        return try {
            val uri = URI(cleaned)
            val host = (uri.host ?: "").lowercase()
            val isX = host == "x.com" ||
                host == "www.x.com" ||
                host == "mobile.x.com" ||
                host == "twitter.com" ||
                host == "www.twitter.com" ||
                host == "mobile.twitter.com"
            if (!isX) return null

            val path = uri.path ?: return null
            val m = statusPath.matcher(path)
            if (!m.find()) {
                // Accept profile or other x links but prefer status for video share.
                if (path.length <= 1) return null
                val canonical = "https://x.com${path.trimEnd('/')}"
                return ParsedXLink(
                    canonicalUrl = canonical,
                    username = path.trim('/').substringBefore('/').takeIf { it.isNotBlank() },
                    statusId = null,
                    raw = url,
                )
            }
            val user = m.group(1)
            val id = m.group(2)
            ParsedXLink(
                canonicalUrl = "https://x.com/$user/status/$id",
                username = user,
                statusId = id,
                raw = url,
            )
        } catch (_: Exception) {
            null
        }
    }

    fun isLikelyXStatus(raw: String): Boolean =
        extract(raw)?.statusId != null
}
