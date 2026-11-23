package com.ismartcoding.plain.tests

import androidx.core.view.descendants
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.rememberCoroutineScope
import androidx.lifecycle.Lifecycle
import com.ismartcoding.plain.ui.platform.media.ExoPlayerVideoController
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.MainActivity
import com.ismartcoding.plain.ui.media.viewer.PreviewItem
import com.ismartcoding.plain.ui.media.viewer.video.MediaVideo
import com.ismartcoding.plain.ui.media.viewer.video.VideoState
import com.ismartcoding.plain.ui.theme.AppTheme
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoFirstFrameTest {
    @Test
    fun knownVideoSizeProducesANonEmptySurfaceOnItsFirstLayout() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File.createTempFile("first-layout-", ".mp4", instrumentation.targetContext.cacheDir)
        try {
            requireNotNull(javaClass.classLoader?.getResourceAsStream("video-start.mp4")).use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            val model = PreviewItem(file.name, file.path, file.length()).apply { intrinsicSize = IntSize(640, 360) }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                val measured = CountDownLatch(1)
                var size = IntSize.Zero
                scenario.onActivity { activity ->
                    val root = activity.window.decorView as android.view.ViewGroup
                    root.viewTreeObserver.addOnGlobalLayoutListener(object : android.view.ViewTreeObserver.OnGlobalLayoutListener {
                        override fun onGlobalLayout() {
                            val surface = root.descendants.filterIsInstance<android.view.TextureView>().firstOrNull() ?: return
                            if (!surface.isLaidOut) return
                            size = IntSize(surface.width, surface.height)
                            root.viewTreeObserver.removeOnGlobalLayoutListener(this)
                            measured.countDown()
                        }
                    })
                    activity.setContent {
                        val state = androidx.compose.runtime.remember { VideoState() }
                        MediaVideo(pagerState = rememberPagerState { 1 }, videoState = state, page = 0, model = model)
                    }
                }
                assertTrue(measured.await(10, TimeUnit.SECONDS))
                assertTrue("First video surface layout was $size", size.width > 0 && size.height > 0)
                assertTrue("First layout must fit the video", kotlin.math.abs(size.width * 9 - size.height * 16) <= 16)
            }
        } finally {
            file.delete()
        }
    }

    @Test
    fun localVideoRendersItsFirstFrameOnOpen() = verifyPlayback(initiallyOpen = true)

    @Test
    fun openingVideoRendersBeforeTheTransitionCanStartPlayback() = verifyPlayback(initiallyOpen = false)

    @Test
    fun onlyTheLastSelectedVideoPlaysAfterReturningToTheActivity() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val files = List(3) { File.createTempFile("switch-", ".mp4", context.cacheDir) }
        try {
            files.forEach { file ->
                requireNotNull(javaClass.classLoader?.getResourceAsStream("video-start.mp4")).use { input ->
                    file.outputStream().use { input.copyTo(it) }
                }
            }
            val items = files.map { PreviewItem(it.name, it.path, it.length()).apply { intrinsicSize = IntSize(640, 360) } }
            val state = VideoState().apply { isPreviewerOpen = true }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                lateinit var pager: PagerState
                lateinit var scope: CoroutineScope
                scenario.onActivity { activity ->
                    activity.setContent {
                        AppTheme(useDarkTheme = false) {
                            pager = rememberPagerState { items.size }
                            scope = rememberCoroutineScope()
                            HorizontalPager(pager, beyondViewportPageCount = 1) { page ->
                                MediaVideo(pagerState = pager, videoState = state, page = page, model = items[page])
                            }
                        }
                    }
                }
                fun awaitPlayer(page: Int): ExoPlayerVideoController {
                    val deadline = SystemClock.elapsedRealtime() + 15000
                    while (SystemClock.elapsedRealtime() < deadline) {
                        var current: ExoPlayerVideoController? = null
                        scenario.onActivity {
                            val player = state.controller as? ExoPlayerVideoController
                            if (player?.isPlaying == true && player.exoPlayer.currentMediaItem?.localConfiguration?.uri?.path == files[page].path) current = player
                        }
                        current?.let { return it }
                        SystemClock.sleep(10)
                    }
                    error("Selected video did not play: $page")
                }
                fun assertSingleVideoSurface() {
                    scenario.onActivity { activity ->
                        val root = activity.window.decorView as android.view.ViewGroup
                        val surfaces = root.descendants.count { it is android.view.TextureView }
                        org.junit.Assert.assertEquals("Offscreen pages must not create video surfaces", 1, surfaces)
                    }
                }
                val first = awaitPlayer(0)
                assertSingleVideoSurface()
                scenario.onActivity { scope.launch { pager.scrollToPage(1) } }
                val second = awaitPlayer(1)
                assertSingleVideoSurface()
                scenario.onActivity { scope.launch { pager.scrollToPage(0) } }
                awaitPlayer(0)
                scenario.onActivity {
                    scope.launch {
                        pager.scrollToPage(1)
                        withFrameNanos { }
                        pager.scrollToPage(2)
                    }
                }
                awaitPlayer(2)
                scenario.moveToState(Lifecycle.State.CREATED)
                scenario.moveToState(Lifecycle.State.RESUMED)
                awaitPlayer(2)
                scenario.onActivity {
                    assertFalse(first.isPlaying)
                    assertFalse(second.isPlaying)
                }
            }
        } finally {
            files.forEach { it.delete() }
        }
    }

    private fun verifyPlayback(initiallyOpen: Boolean) {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File.createTempFile("first-frame-", ".mp4", instrumentation.targetContext.cacheDir)
        try {
            requireNotNull(javaClass.classLoader?.getResourceAsStream("video-start.mp4")).use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            val model = PreviewItem(file.name, file.path, file.length()).apply { intrinsicSize = IntSize(640, 360) }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                val rendered = CountDownLatch(1)
                val state = VideoState().apply { isPreviewerOpen = initiallyOpen }
                scenario.onActivity { activity ->
                    val start = SystemClock.elapsedRealtime()
                    activity.setContent {
                        AppTheme(useDarkTheme = false) {
                            MediaVideo(
                                modifier = Modifier.fillMaxSize(),
                                pagerState = rememberPagerState { 1 },
                                videoState = state,
                                page = 0,
                                model = model,
                                onMounted = {
                                    android.util.Log.i("VideoFirstFrameTest", "Open to first frame: ${SystemClock.elapsedRealtime() - start} ms")
                                    rendered.countDown()
                                },
                            )
                        }
                    }
                }
                assertTrue("Expected a decoded video frame", rendered.await(15, TimeUnit.SECONDS))
                if (!initiallyOpen) scenario.onActivity {
                    assertNull(state.controller)
                    assertFalse(state.isPlaying)
                }
            }
        } finally {
            file.delete()
        }
    }
}
