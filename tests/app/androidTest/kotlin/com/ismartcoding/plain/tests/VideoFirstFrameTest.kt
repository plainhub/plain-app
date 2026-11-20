package com.ismartcoding.plain.tests

import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.remember
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
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class VideoFirstFrameTest {
    @Test
    fun localVideoRendersItsFirstFrameOnOpen() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val file = File.createTempFile("first-frame-", ".mp4", instrumentation.targetContext.cacheDir)
        try {
            requireNotNull(javaClass.classLoader?.getResourceAsStream("video-start.mp4")).use { input ->
                file.outputStream().use { input.copyTo(it) }
            }
            val model = PreviewItem(file.name, file.path, file.length()).apply { intrinsicSize = IntSize(640, 360) }
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                val rendered = CountDownLatch(1)
                scenario.onActivity { activity ->
                    val start = SystemClock.elapsedRealtime()
                    activity.setContent {
                        AppTheme(useDarkTheme = false) {
                            MediaVideo(
                                modifier = Modifier.fillMaxSize(),
                                pagerState = rememberPagerState { 1 },
                                videoState = remember { VideoState().apply { isPreviewerOpen = true } },
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
            }
        } finally {
            file.delete()
        }
    }
}
