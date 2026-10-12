package com.gglee.xhotpost

import com.gglee.xhotpost.domain.AppSettings
import com.gglee.xhotpost.domain.DraftGenerator
import com.gglee.xhotpost.domain.MediaLinkParser
import com.gglee.xhotpost.domain.VideoPlatform
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MediaLinkParserTest {
    @Test
    fun extract_xStatus() {
        val link = MediaLinkParser.extract(
            "看这个 https://x.com/demo/status/1234567890123456789 不错",
        )
        assertNotNull(link)
        assertEquals(VideoPlatform.X, link!!.platform)
        assertEquals("1234567890123456789", link.id)
    }

    @Test
    fun extract_youtubeWatchAndShorts() {
        val watch = MediaLinkParser.extract(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ&t=10s",
        )
        assertNotNull(watch)
        assertEquals(VideoPlatform.YOUTUBE, watch!!.platform)
        assertEquals("dQw4w9WgXcQ", watch.id)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", watch.canonicalUrl)

        val shorts = MediaLinkParser.extract("https://youtube.com/shorts/abcdefghijk")
        assertEquals(VideoPlatform.YOUTUBE, shorts!!.platform)
        assertEquals("abcdefghijk", shorts.id)

        val be = MediaLinkParser.extract("https://youtu.be/abcdefghijk")
        assertEquals("abcdefghijk", be!!.id)
    }

    @Test
    fun extract_douyinVideoAndShort() {
        val video = MediaLinkParser.extract(
            "https://www.douyin.com/video/7123456789012345678",
        )
        assertNotNull(video)
        assertEquals(VideoPlatform.DOUYIN, video!!.platform)
        assertEquals("7123456789012345678", video.id)

        val short = MediaLinkParser.extract("https://v.douyin.com/iAbcDef/")
        assertNotNull(short)
        assertEquals(VideoPlatform.DOUYIN, short!!.platform)
        assertTrue(short.id.startsWith("short:"))
    }

    @Test
    fun reject_unknownHost() {
        assertNull(MediaLinkParser.extract("https://example.com/video/1"))
    }

    @Test
    fun mediaShareCaption_stripsUrls() {
        val link = MediaLinkParser.extract(
            "https://www.youtube.com/watch?v=dQw4w9WgXcQ",
        )!!
        val text = DraftGenerator.mediaShareCaptionText(
            link,
            "不错的一条 https://www.youtube.com/watch?v=dQw4w9WgXcQ",
            AppSettings(),
        )
        assertFalse(text.contains("https://"))
        assertTrue(text.contains("不错"))
    }
}
