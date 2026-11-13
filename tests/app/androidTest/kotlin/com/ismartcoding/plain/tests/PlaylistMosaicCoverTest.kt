package com.ismartcoding.plain.tests

import android.graphics.Bitmap
import android.graphics.Color
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.MainActivity
import com.ismartcoding.plain.models.AudioAlbumCover
import com.ismartcoding.plain.ui.audio.components.audioCoverCache
import com.ismartcoding.plain.ui.audio.playlists.components.PlaylistMosaicCover
import com.ismartcoding.plain.ui.theme.AppTheme
import java.io.File
import java.util.concurrent.atomic.AtomicReference
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PlaylistMosaicCoverTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private fun cacheKey(path: String) = com.ismartcoding.plain.rust.graphql.ContentSource.Local.key + ":" + path
    private val colors = listOf(Color.RED, Color.GREEN, Color.BLUE, Color.YELLOW, Color.MAGENTA)

    @Test
    fun zero_to_five_covers_keep_the_existing_tiles_fallbacks_and_cache() {
        val bounds = AtomicReference<androidx.compose.ui.geometry.Rect>()
        val window = AtomicReference<android.view.Window>()
        val audio = File.createTempFile("playlist-cover-", ".mp3", instrumentation.targetContext.cacheDir)
        checkNotNull(javaClass.classLoader?.getResourceAsStream("playlist-cover-red.mp3")).use { input -> audio.outputStream().use { input.copyTo(it) } }
        kotlinx.coroutines.runBlocking {
            org.junit.Assert.assertNotNull("Generated MP3 artwork must decode", com.ismartcoding.plain.ui.platform.media.loadAudioCoverBitmap(audio.path))
        }
        val paths = colors.indices.map { if (it == 0) audio.path else "playlist-cover-test-$it" }
        val missing = "playlist-cover-test-missing"
        val albums = paths.mapIndexed { index, path ->
            AudioAlbumCover("album-$index", 4, listOf(missing, path))
        }
        instrumentation.runOnMainSync {
            audioCoverCache[cacheKey(missing)] = null
            colors.forEachIndexed { index, color ->
                if (index > 0) audioCoverCache[cacheKey(paths[index])] = Bitmap.createBitmap(8, 8, Bitmap.Config.ARGB_8888).apply { eraseColor(color) }.asImageBitmap()
            }
        }
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                fun render(selected: List<AudioAlbumCover>) {
                    scenario.onActivity { activity ->
                        window.set(activity.window)
                        activity.setContent {
                            AppTheme(useDarkTheme = false) {
                                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                                    PlaylistMosaicCover(selected, 2, Modifier.size(128.dp).onGloballyPositioned { bounds.set(it.boundsInWindow()) })
                                }
                            }
                        }
                    }
                    instrumentation.waitForIdleSync()
                    val frame = java.util.concurrent.CountDownLatch(1)
                    scenario.onActivity { activity ->
                        activity.window.decorView.postOnAnimation {
                            activity.window.decorView.postOnAnimation { frame.countDown() }
                        }
                    }
                    assertTrue("Cover frame did not reach the display", frame.await(5, java.util.concurrent.TimeUnit.SECONDS))
                }
                fun capture(): Bitmap {
                    repeat(100) {
                        val rectangle = bounds.get()
                        if (rectangle != null) {
                            val result = Bitmap.createBitmap(rectangle.width.toInt(), rectangle.height.toInt(), Bitmap.Config.ARGB_8888)
                            val copied = java.util.concurrent.CountDownLatch(1)
                            val status = java.util.concurrent.atomic.AtomicInteger(-1)
                            android.view.PixelCopy.request(
                                checkNotNull(window.get()),
                                android.graphics.Rect(rectangle.left.toInt(), rectangle.top.toInt(), rectangle.right.toInt(), rectangle.bottom.toInt()),
                                result,
                                { value -> status.set(value); copied.countDown() },
                                android.os.Handler(android.os.Looper.getMainLooper()),
                            )
                            assertTrue("Cover window was not copied", copied.await(5, java.util.concurrent.TimeUnit.SECONDS))
                            assertEquals(android.view.PixelCopy.SUCCESS, status.get())
                            return result
                        }
                        Thread.sleep(50)
                    }
                    error("Playlist cover did not render")
                }
                fun pixels(bitmap: Bitmap): IntArray = IntArray(bitmap.width * bitmap.height).also {
                    bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height)
                }
                render(emptyList())
                val placeholder = capture()
                render(listOf(AudioAlbumCover("missing", 1, listOf(missing))))
                val unavailable = capture()
                assertArrayEquals(pixels(placeholder), pixels(unavailable))
                placeholder.recycle()
                unavailable.recycle()
                var decoded: androidx.compose.ui.graphics.ImageBitmap? = null
                for (count in 1..5) {
                    render(albums.take(count))
                    val expected = when (count) {
                        1 -> listOf(colors[0], colors[0], colors[0], colors[0])
                        2 -> listOf(colors[0], colors[1], colors[0], colors[1])
                        3 -> listOf(colors[0], colors[1], colors[0], colors[2])
                        else -> colors.take(4)
                    }
                    var matched: Bitmap? = null
                    repeat(100) {
                        if (matched == null) {
                            val bitmap = capture()
                            val samples = listOf(1 to 1, 3 to 1, 1 to 3, 3 to 3).map { (x, y) -> bitmap.getPixel(bitmap.width * x / 4, bitmap.height * y / 4) }
                            if (samples.zip(expected).all { (actual, color) ->
                                kotlin.math.abs(Color.red(actual) - Color.red(color)) <= 3 &&
                                    kotlin.math.abs(Color.green(actual) - Color.green(color)) <= 3 &&
                                    kotlin.math.abs(Color.blue(actual) - Color.blue(color)) <= 3
                            }) matched = bitmap else bitmap.recycle()
                            if (matched == null) Thread.sleep(50)
                        }
                    }
                    if (matched == null) {
                        val failed = capture()
                        File(instrumentation.targetContext.cacheDir, "playlist-mosaic-failed-$count.png").outputStream().use { failed.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        val screen = checkNotNull(instrumentation.uiAutomation.takeScreenshot())
                        File(instrumentation.targetContext.cacheDir, "playlist-mosaic-failed-screen.png").outputStream().use { screen.compress(Bitmap.CompressFormat.PNG, 100, it) }
                        val samples = listOf(1 to 1, 3 to 1, 1 to 3, 3 to 3).map { (x, y) -> failed.getPixel(failed.width * x / 4, failed.height * y / 4).toUInt().toString(16) }
                        failed.recycle()
                        screen.recycle()
                        error("Incorrect mosaic for $count albums: pixels=$samples cached=${audioCoverCache[cacheKey(audio.path)] != null} bounds=${bounds.get()}")
                    }
                    val bitmap = checkNotNull(matched)
                    val cached = checkNotNull(audioCoverCache[cacheKey(audio.path)])
                    if (decoded == null) decoded = cached else assertSame(decoded, cached)
                    File(instrumentation.targetContext.cacheDir, "playlist-mosaic-$count.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
                    bitmap.recycle()
                }
                assertEquals(6, paths.count { audioCoverCache.containsKey(cacheKey(it)) } + if (audioCoverCache.containsKey(cacheKey(missing))) 1 else 0)
            }
        } finally {
            assertTrue(audio.delete())
            instrumentation.runOnMainSync {
                paths.forEach { audioCoverCache.remove(cacheKey(it)) }
                audioCoverCache.remove(cacheKey(missing))
            }
        }
    }
}
