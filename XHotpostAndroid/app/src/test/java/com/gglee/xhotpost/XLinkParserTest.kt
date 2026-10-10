package com.gglee.xhotpost

import com.gglee.xhotpost.domain.AppSettings
import com.gglee.xhotpost.domain.DraftGenerator
import com.gglee.xhotpost.domain.XLinkParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XLinkParserTest {
    @Test
    fun extract_statusUrl_fromClipboardBlurb() {
        val raw = "看看这个 https://x.com/elonmusk/status/1234567890123456789?s=20 不错"
        val parsed = XLinkParser.extract(raw)
        assertNotNull(parsed)
        assertEquals("elonmusk", parsed!!.username)
        assertEquals("1234567890123456789", parsed.statusId)
        assertEquals(
            "https://x.com/elonmusk/status/1234567890123456789",
            parsed.canonicalUrl,
        )
    }

    @Test
    fun extract_twitterMobile() {
        val parsed = XLinkParser.extract(
            "https://mobile.twitter.com/OpenAI/status/9876543210",
        )
        assertNotNull(parsed)
        assertEquals("OpenAI", parsed!!.username)
        assertTrue(parsed.canonicalUrl.startsWith("https://x.com/"))
    }

    @Test
    fun reject_nonStatus() {
        assertNull(XLinkParser.extract("https://example.com/video/1"))
        assertFalse(XLinkParser.isLikelyXStatus("https://x.com/home"))
    }

    @Test
    fun linkSharePost_appendsUrlOnce() {
        val parsed = XLinkParser.extract(
            "https://x.com/demo/status/111",
        )!!
        val text = DraftGenerator.linkSharePost(parsed, "值得对照一下。", AppSettings())
        assertTrue(text.contains("https://x.com/demo/status/111"))
        assertEquals(1, Regex("https://x.com/demo/status/111").findAll(text).count())
        assertTrue(text.length <= 280)
    }
}
