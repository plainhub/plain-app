package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.helpers.Mp4Helper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * End-to-end device test for the /fs HEVC→H.264 fallback pipeline, using a
 * real Pixel-camera recording pushed to /data/local/tmp/hevc_probe.mp4:
 *
 *   adb push PXL_xxx.TS.mp4 /data/local/tmp/hevc_probe.mp4
 *   ./gradlew :app:connectedGithubDebugAndroidTest \
 *     -Pandroid.testInstrumentationRunnerArguments.class=com.ismartcoding.plain.tests.Mp4TranscodeInstrumentedTest
 *
 * Asserts the transcode produces an AVC+AAC file matching the source
 * duration. Timing is logged so regressions in the realtime factor show up
 * in test output.
 */
@RunWith(AndroidJUnit4::class)
class Mp4TranscodeInstrumentedTest {

    private fun fixture(): File {
        val src = File("/data/local/tmp/hevc_probe.mp4")
        org.junit.Assume.assumeTrue("fixture missing — adb push first", src.exists())
        return src
    }

    @Test
    fun transcodesHevcToPlayableAvc() {
        val src = fixture()
        assertTrue("fixture is not HEVC", Mp4Helper.isHevc(src.absolutePath))

        val context = InstrumentationRegistry.getInstrumentation().targetContext
        // Drop any cached result from a previous run.
        File(context.cacheDir, "remux").listFiles()
            ?.filter { it.name.startsWith("webtc_") }
            ?.forEach { it.delete() }

        val start = System.currentTimeMillis()
        val out = Mp4Helper.transcodeForBrowser(context, src.absolutePath)
        val elapsedMs = System.currentTimeMillis() - start

        assertNotNull("transcodeForBrowser returned null (pipeline failed)", out)
        out!!
        val outFile = File(out)
        assertTrue("output too small: ${outFile.length()}", outFile.length() > 100_000)
        assertEquals("output video codec", "avc1", Mp4Helper.firstVideoSampleEntry(out))
        val srcDuration = Mp4Helper.getMp4Duration(src.absolutePath)
        val outDuration = Mp4Helper.getMp4Duration(out)
        assertTrue(
            "duration drift: src=$srcDuration out=$outDuration",
            Math.abs(srcDuration - outDuration) <= 2,
        )
        // 12 s / ~21 Mbps source: the non-blocking feed/drain loop should
        // finish well under 10 s on a Tensor-class device (regression guard
        // for the 10 ms-blocking-per-buffer iteration that measured 21 s).
        assertTrue("transcode too slow: ${elapsedMs}ms", elapsedMs < 10_000)
        // Expose the artifact for host-side ffprobe via MediaStore (no
        // runtime permission needed for app-created rows; AGP uninstalls the
        // app after the run, so run-as pulls are not possible).
        val values = android.content.ContentValues().apply {
            put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME, "webtc_out.mp4")
            put(android.provider.MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH, "Download")
        }
        val uri = context.contentResolver.insert(
            android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI, values)
        assertNotNull(uri)
        context.contentResolver.openOutputStream(uri!!)!!.use { it.write(outFile.readBytes()) }
        println("TRANSCODE_MS=$elapsedMs SRC_BYTES=${src.length()} OUT_BYTES=${outFile.length()} OUT_PATH=$out")
    }

    @Test
    fun secondCallHitsCache() {
        val src = fixture()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val first = Mp4Helper.transcodeForBrowser(context, src.absolutePath)
        assertNotNull(first)
        val start = System.currentTimeMillis()
        val second = Mp4Helper.transcodeForBrowser(context, src.absolutePath)
        val elapsedMs = System.currentTimeMillis() - start
        assertNotNull(second)
        assertEquals(first, second)
        assertTrue("cache hit too slow: ${elapsedMs}ms", elapsedMs < 2000)
    }
}
