package com.ismartcoding.plain.ui.home

import com.ismartcoding.plain.models.MediaBucket
import com.ismartcoding.plain.ui.components.sidebarBucketThumbnailPaths
import com.ismartcoding.plain.ui.media.OnceOnOpenLoader
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class OnceOnOpenLoaderTest {
    @Test
    fun loadedDrawersRetainFolderCoversAcrossCloseAndReopen() {
        val paths = listOf("/fixture/one.jpg", "/fixture/two.jpg")
        val bucket = MediaBucket("fixture", "Fixture", paths.size, paths)
        val loader = OnceOnOpenLoader()
        val coversAcrossToggles = listOf(false, true, false, true).map { isOpen ->
            loader.shouldLoad(isOpen)
            sidebarBucketThumbnailPaths(bucket, isMedia = true, coversEnabled = loader.hasOpened)
        }
        assertEquals(listOf(emptyList(), paths, paths, paths), coversAcrossToggles)
        assertEquals(emptyList(), sidebarBucketThumbnailPaths(bucket, isMedia = false, coversEnabled = loader.hasOpened))
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
