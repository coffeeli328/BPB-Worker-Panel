package com.gglee.xhotpost

import com.gglee.xhotpost.domain.XLinkParser
import com.gglee.xhotpost.domain.XVideoFetcher
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class XVideoFetcherTest {
    @Test
    fun pickBestMp4Url_prefersMidBitrate() {
        val json = """
            {"formats":[
              {"url":"https://video.twimg.com/x/low.mp4","bitrate":256000},
              {"url":"https://video.twimg.com/x/mid.mp4","bitrate":2176000},
              {"url":"https://video.twimg.com/x/hi.mp4","bitrate":10368000}
            ]}
        """.trimIndent()
        val url = XVideoFetcher.pickBestMp4Url(json)
        assertNotNull(url)
        assertTrue(url!!.contains("mid.mp4"))
    }

    @Test
    fun parseFxTwitter_picksMp4() {
        val parsed = XLinkParser.extract("https://x.com/Starlink/status/2059391182094880785")!!
        val json = """
            {
              "code": 200,
              "tweet": {
                "text": "demo clip",
                "author": { "screen_name": "Starlink" },
                "media": {
                  "videos": [{
                    "id": "1",
                    "url": "https://video.twimg.com/amplify_video/1/vid/avc1/640x360/a.mp4?tag=27",
                    "duration": 17.2,
                    "width": 640,
                    "height": 360,
                    "type": "video",
                    "formats": [
                      {"url": "https://video.twimg.com/x/low.mp4", "bitrate": 256000},
                      {"url": "https://video.twimg.com/x/mid.mp4", "bitrate": 2176000}
                    ]
                  }]
                }
              }
            }
        """.trimIndent()
        val info = XVideoFetcher.parseFxTwitter(json, parsed)
        assertNotNull(info)
        assertTrue(info!!.mp4Url.contains(".mp4"))
    }

    @Test
    fun parseFxTwitter_ignoresPhotosOnly() {
        val parsed = XLinkParser.extract("https://x.com/a/status/1")!!
        val json = """
            {"tweet":{"media":{"all":[{"type":"photo","url":"https://pbs.twimg.com/media/x.jpg"}]}}}
        """.trimIndent()
        assertNull(XVideoFetcher.parseFxTwitter(json, parsed))
    }
}
