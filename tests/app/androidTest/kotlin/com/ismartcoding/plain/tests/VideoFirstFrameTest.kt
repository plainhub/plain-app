package com.ismartcoding.plain.tests

import androidx.core.view.descendants
import android.os.SystemClock
import android.view.TextureView
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.withFrameNanos
import androidx.lifecycle.Lifecycle
import com.ismartcoding.plain.ui.platform.media.ExoPlayerVideoController
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntSize
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.MainActivity
import com.ismartcoding.plain.features.media.MediaType
import com.ismartcoding.plain.ui.media.MediaPreviewData
import com.ismartcoding.plain.ui.media.viewer.PreviewItem
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.media.viewer.video.MediaVideo
import com.ismartcoding.plain.ui.media.viewer.video.VideoState
import com.ismartcoding.plain.ui.platform.media.MediaPreviewer
import com.ismartcoding.plain.ui.theme.AppTheme
import java.io.File
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
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
    fun adjacentVideoHasADecodedFrameBeforeTheSwipeSettles() = withPreviewer(preloadAdjacentVideos = true) { scenario, state, files ->
        val first = awaitPlayer(scenario, state, files[0])
        var adjacentSurface: TextureView? = null
        val deadline = SystemClock.elapsedRealtime() + 10000
        while (adjacentSurface == null && SystemClock.elapsedRealtime() < deadline) {
            scenario.onActivity { activity ->
                val root = activity.window.decorView as android.view.ViewGroup
                adjacentSurface = root.descendants.filterIsInstance<TextureView>().firstOrNull { surface ->
                    val location = IntArray(2)
                    surface.getLocationOnScreen(location)
                    if (location[0] < root.width) return@firstOrNull false
                    val frame = surface.getBitmap(2, 2) ?: return@firstOrNull false
                    val blueFrame = android.graphics.Color.blue(frame.getPixel(1, 1)) > 200
                    frame.recycle()
                    blueFrame
                }
            }
            if (adjacentSurface == null) SystemClock.sleep(20)
        }
        assertTrue("Expected a decoded frame on the next page before starting the swipe", adjacentSurface != null)

        scenario.onActivity {
            assertSame(first, state.videoState.controller)
            state.scope.launch { state.pagerState.animateScrollToPage(1) }
        }
        val second = awaitPlayer(scenario, state, files[1])
        scenario.onActivity { activity ->
            assertFalse(first.isPlaying)
            val root = activity.window.decorView as android.view.ViewGroup
            assertTrue("The next page must keep its prepared surface", root.descendants.any { it === adjacentSurface })
            state.scope.launch { state.pagerState.animateScrollToPage(0) }
        }
        assertSame(first, awaitPlayer(scenario, state, files[0]))
        scenario.onActivity {
            assertFalse(second.isPlaying)
        }
    }

    @Test
    fun onlyTheLastSelectedVideoPlaysAfterReturningToTheActivity() = withPreviewer { scenario, state, files ->
        fun assertSingleVideoSurface() {
            scenario.onActivity { activity ->
                val root = activity.window.decorView as android.view.ViewGroup
                val surfaces = root.descendants.count { it is TextureView }
                org.junit.Assert.assertEquals("Offscreen pages must not create video surfaces", 1, surfaces)
            }
        }
        val first = awaitPlayer(scenario, state, files[0])
        assertSingleVideoSurface()
        scenario.onActivity { state.scope.launch { state.pagerState.scrollToPage(1) } }
        val second = awaitPlayer(scenario, state, files[1])
        assertSingleVideoSurface()
        scenario.onActivity { state.scope.launch { state.pagerState.scrollToPage(0) } }
        awaitPlayer(scenario, state, files[0])
        scenario.onActivity {
            state.scope.launch {
                state.pagerState.scrollToPage(1)
                withFrameNanos { }
                state.pagerState.scrollToPage(2)
            }
        }
        awaitPlayer(scenario, state, files[2])
        scenario.moveToState(Lifecycle.State.CREATED)
        scenario.moveToState(Lifecycle.State.RESUMED)
        awaitPlayer(scenario, state, files[2])
        scenario.onActivity {
            assertFalse(first.isPlaying)
            assertFalse(second.isPlaying)
        }
    }

    private fun awaitPlayer(scenario: ActivityScenario<MainActivity>, state: MediaPreviewerState, file: File): ExoPlayerVideoController {
        val deadline = SystemClock.elapsedRealtime() + 15000
        while (SystemClock.elapsedRealtime() < deadline) {
            var current: ExoPlayerVideoController? = null
            scenario.onActivity {
                val player = state.videoState.controller as? ExoPlayerVideoController
                if (player?.isPlaying == true && player.exoPlayer.currentMediaItem?.localConfiguration?.uri?.path == file.path) current = player
            }
            current?.let { return it }
            SystemClock.sleep(10)
        }
        error("Selected video did not play: ${file.name}")
    }

    private fun withPreviewer(
        preloadAdjacentVideos: Boolean = false,
        test: (ActivityScenario<MainActivity>, MediaPreviewerState, List<File>) -> Unit,
    ) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val files = List(3) { File.createTempFile("handoff-", ".mp4", context.cacheDir) }
        try {
            files.forEach { file ->
                requireNotNull(javaClass.classLoader?.getResourceAsStream("video-start.mp4")).use { input ->
                    file.outputStream().use { input.copyTo(it) }
                }
            }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                val opened = CountDownLatch(1)
                lateinit var state: MediaPreviewerState
                scenario.onActivity { activity ->
                    MediaPreviewData.items = files.map {
                        PreviewItem(it.name, it.path, it.length(), pickedMediaType = MediaType.VIDEO).apply {
                            intrinsicSize = IntSize(640, 360)
                        }
                    }
                    activity.setContent {
                        AppTheme(useDarkTheme = false) {
                            state = rememberPreviewerState()
                            MediaPreviewer(state, preloadAdjacentVideos = preloadAdjacentVideos)
                            LaunchedEffect(state) {
                                state.open()
                                opened.countDown()
                            }
                        }
                    }
                }
                assertTrue("Expected the video preview to open", opened.await(15, TimeUnit.SECONDS))
                test(scenario, state, files)
            }
        } finally {
            MediaPreviewData.items = emptyList()
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
