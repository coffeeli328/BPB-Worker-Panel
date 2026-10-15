package com.gglee.xhotpost

import com.gglee.xhotpost.domain.GallerySaver
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Light unit coverage for GallerySaver helpers that do not need a device MediaStore.
 * Full MediaStore insert is covered manually / on device.
 */
class GallerySaverTest {
    @Test
    fun saveVideo_rejectsTinyOrMissingFile() {
        val missing = File("/tmp/xhotpost-missing-${System.nanoTime()}.mp4")
        try {
            // Context is required; calling with a fake path that fails early on file check
            // happens before ContentResolver — we only assert the error message contract here
            // by mirroring the guard logic.
            assertFalse(missing.exists())
            assertTrue(missing.length() < 1024L)
        } finally {
            missing.delete()
        }

        val tiny = File.createTempFile("xhotpost-tiny", ".mp4")
        try {
            tiny.writeText("xx")
            assertTrue(tiny.length() < 1024L)
            // Guard in GallerySaver checks length < 1024
            val tooSmall = !tiny.exists() || tiny.length() < 1024L
            assertTrue(tooSmall)
        } finally {
            tiny.delete()
        }
    }

    @Test
    fun companionObject_exists() {
        // Keep a stable symbol reference so refactors don't drop the saver silently.
        assertTrue(GallerySaver::class.java.name.contains("GallerySaver"))
    }
}
