package com.ismartcoding.plain.ui.home

import com.ismartcoding.plain.models.MediaBucket
import com.ismartcoding.plain.models.MediaBucketCover
import kotlin.time.Instant
import com.ismartcoding.plain.ui.components.sidebarBucketCovers
import com.ismartcoding.plain.ui.media.OnceOnOpenLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnceOnOpenLoaderTest {
    @Test
    fun loadedDrawersRetainFolderCoversAcrossCloseAndReopen() {
        val paths = listOf("/fixture/one.jpg", "/fixture/two.jpg")
        val covers = paths.map { MediaBucketCover(it, Instant.fromEpochMilliseconds(0), null) }
        val bucket = MediaBucket("fixture", "Fixture", covers.size, covers)
        val loader = OnceOnOpenLoader()
        val coversAcrossToggles = listOf(false, true, false, true).map { isOpen ->
            loader.shouldLoad(isOpen)
            sidebarBucketCovers(bucket, isMedia = true, coversEnabled = loader.hasOpened)
        }
        assertEquals(listOf(emptyList(), covers, covers, covers), coversAcrossToggles)
        assertEquals(emptyList(), sidebarBucketCovers(bucket, isMedia = false, coversEnabled = loader.hasOpened))
    }

    @Test
    fun closedDrawerNeverFires() {
        val loader = OnceOnOpenLoader()
        assertFalse(loader.shouldLoad(isOpen = false))
        assertFalse(loader.shouldLoad(isOpen = false))
    }

    @Test
    fun firstOpenFiresExactlyOnce() {
        val loader = OnceOnOpenLoader()
        assertTrue(loader.shouldLoad(isOpen = true))
        assertFalse(loader.shouldLoad(isOpen = true))
    }

    @Test
    fun reopenAfterCloseDoesNotRefire() {
        val loader = OnceOnOpenLoader()
        assertFalse(loader.shouldLoad(isOpen = false))
        assertTrue(loader.shouldLoad(isOpen = true))
        assertFalse(loader.shouldLoad(isOpen = false))
        assertFalse(loader.shouldLoad(isOpen = true))
    }

    @Test
    fun closedObservationsBeforeFirstOpenDoNotConsumeTheSingleFire() {
        val loader = OnceOnOpenLoader()
        loader.shouldLoad(isOpen = false)
        loader.shouldLoad(isOpen = false)
        assertTrue(loader.shouldLoad(isOpen = true))
    }
}
