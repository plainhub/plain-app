package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.features.VideoProgressHelper
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

@RunWith(AndroidJUnit4::class)
class VideoProgressRustHttpTest {
    @Test
    fun videoPositionUsesRustAndRetainsLongMilliseconds() = runBlocking {
        val id = "video-rust-${UUID.randomUUID()}"
        try {
            val row = VideoProgressHelper.saveAsync(id, 3_000_000_123)
            assertEquals(3_000_000_123, checkNotNull(VideoProgressHelper.getAsync(id)).positionMs)
            assertTrue(VideoProgressHelper.recentAsync(Clock.System.now() - 1.days).any { it.mediaId == id && it.updatedAt == row.updatedAt })
            assertTrue(runCatching { VideoProgressHelper.saveAsync(id, -1) }.isFailure)
            assertEquals(3_000_000_123, checkNotNull(VideoProgressHelper.getAsync(id)).positionMs)
            VideoProgressHelper.saveAsync(id, 0)
            assertEquals(0L, checkNotNull(VideoProgressHelper.getAsync(id)).positionMs)
            VideoProgressHelper.deleteAsync(id)
            assertNull(VideoProgressHelper.getAsync(id))
        } finally {
            VideoProgressHelper.deleteAsync(id)
        }
    }
}
