package com.ismartcoding.plain.tests

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Rect
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.PixelCopy
import android.view.View.MeasureSpec
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.ismartcoding.plain.MainActivity
import com.ismartcoding.plain.models.MediaBucketCover
import com.ismartcoding.plain.ui.media.MediaGridThumbnail
import com.ismartcoding.plain.ui.media.MediaThumbnailGrid
import com.ismartcoding.plain.ui.media.ThumbnailPlaceholder
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberTransformItemState
import com.ismartcoding.plain.ui.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.concurrent.CompletableFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.io.path.createTempDirectory
import kotlin.math.roundToInt
import kotlin.time.Instant

@RunWith(AndroidJUnit4::class)
class MediaGridPlaceholderTest {
    private val instrumentation = InstrumentationRegistry.getInstrumentation()

    @Test
    fun firstDrawShowsTheThumbHash() {
        val image = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                scenario.onActivity { activity ->
                    val view = ComposeView(activity)
                    view.setContent {
                        AppTheme(useDarkTheme = false, amoledDarkTheme = false) {
                            ThumbnailPlaceholder("2BoDBwAAAAAAAAAAAAAAAAAAAAAAAAAA", Modifier.fillMaxSize())
                        }
                    }
                    activity.setContentView(view)
                    val size = MeasureSpec.makeMeasureSpec(image.width, MeasureSpec.EXACTLY)
                    view.measure(size, size)
                    view.layout(0, 0, image.width, image.height)
                    view.draw(Canvas(image))
                }
                assertEquals(Color.rgb(199, 57, 35), image.getPixel(32, 32))
            }
        } finally {
            image.recycle()
        }
    }

    @Test
    fun gridShowsThemeColorAndThumbHashUntilThumbnailsAreAvailable() {
        val folder = createTempDirectory(instrumentation.targetContext.cacheDir.toPath(), "grid-placeholder-").toFile()
        val missingImage = File(folder, "missing.png")
        val missingVideo = File(folder, "missing.mp4")
        val validImage = File(folder, "preview.png")
        val previewColor = Color.rgb(23, 118, 86)
        val thumbHash = "2BoDBwAAAAAAAAAAAAAAAAAAAAAAAAAA"
        val updatedAt = Instant.fromEpochMilliseconds(0)
        val image = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        image.eraseColor(previewColor)
        validImage.outputStream().use { image.compress(Bitmap.CompressFormat.PNG, 100, it) }
        image.recycle()
        val dark = mutableStateOf(false)
        val amoled = mutableStateOf(false)
        val sample = AtomicReference<Rect>()

        try {
            ActivityScenario.launch(MainActivity::class.java).use { scenario ->
                lateinit var activity: MainActivity
                scenario.onActivity {
                    activity = it
                    it.setContent {
                        AppTheme(useDarkTheme = dark.value, amoledDarkTheme = amoled.value) {
                            Row(
                                Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background),
                                horizontalArrangement = Arrangement.Center,
                            ) {
                                Row(Modifier.onGloballyPositioned { coordinates ->
                                    val start = coordinates.positionInWindow()
                                    val side = coordinates.size.width / 4
                                    val left = start.x.roundToInt()
                                    val top = start.y.roundToInt()
                                    sample.set(Rect(left, top + side / 4, left + side * 4, top + side * 3 / 4))
                                }) {
                                    for (file in listOf(missingImage, missingVideo, validImage)) {
                                        androidx.compose.foundation.layout.Box(Modifier.width(72.dp)) {
                                            MediaGridThumbnail(
                                                uri = file.path,
                                                path = file.path,
                                                key = file.name,
                                                thumbHash = if (file == missingImage) null else thumbHash,
                                                updatedAt = updatedAt,
                                                itemState = rememberTransformItemState(),
                                                previewerState = rememberPreviewerState(),
                                            )
                                        }
                                    }
                                    MediaThumbnailGrid(
                                        items = listOf(MediaBucketCover(missingVideo.path, updatedAt, thumbHash)),
                                        contentDescription = null,
                                        modifier = Modifier.size(72.dp),
                                    )
                                }
                            }
                        }
                    }
                }
                assertGrid(activity, sample, Color.rgb(227, 225, 241), previewColor)
                scenario.onActivity { dark.value = true }
                assertGrid(activity, sample, Color.rgb(51, 52, 64), previewColor)
                scenario.onActivity { amoled.value = true }
                assertGrid(activity, sample, Color.rgb(51, 52, 64), previewColor)
            }
        } finally {
            folder.deleteRecursively()
        }
    }

    private fun assertGrid(
        activity: MainActivity,
        sample: AtomicReference<Rect>,
        placeholder: Int,
        previewColor: Int,
    ) {
        val deadline = SystemClock.elapsedRealtime() + 5_000
        var observed: List<Int>? = null
        val hashColor = Color.rgb(199, 57, 35)
        val wanted = listOf(placeholder, hashColor, previewColor, hashColor)
        while (SystemClock.elapsedRealtime() < deadline) {
            instrumentation.waitForIdleSync()
            val bounds = sample.get()
            if (bounds != null) {
                val bitmap = Bitmap.createBitmap(bounds.width(), bounds.height(), Bitmap.Config.ARGB_8888)
                try {
                    val result = CompletableFuture<Int>()
                    instrumentation.runOnMainSync {
                        PixelCopy.request(activity.window, bounds, bitmap, { result.complete(it) }, Handler(Looper.getMainLooper()))
                    }
                    assertEquals(PixelCopy.SUCCESS, result.get(3, TimeUnit.SECONDS).toInt())
                    val side = bitmap.width / 4
                    observed = List(4) { tile ->
                        val x = tile * side + side / 4
                        val y = bitmap.height / 2
                        val pixel = bitmap.getPixel(x, y)
                        if ((x until x + side / 2).all { col ->
                                (y - bitmap.height / 4 until y + bitmap.height / 4).all { row ->
                                    bitmap.getPixel(col, row) == pixel
                                }
                            }
                        ) pixel else null
                    }.map { it ?: Color.TRANSPARENT }
                    if (observed == wanted) return
                } finally {
                    bitmap.recycle()
                }
            }
            SystemClock.sleep(50)
        }
        assertEquals(wanted, observed)
    }
}
