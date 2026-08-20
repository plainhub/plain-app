package com.ismartcoding.plain.features.share

import com.ismartcoding.plain.db.DShare
import com.ismartcoding.plain.db.ShareRoot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.time.Instant

class ShareManagerTest {
    private val photosRoot = ShareRoot(
        virtualPath = "photos/",
        realPath = "/storage/emulated/0/photos",
        isDir = true,
    )

    private fun share(roots: List<ShareRoot>, expiresAt: Instant? = null): DShare =
        DShare(id = "test-share").apply {
            this.expiresAt = expiresAt
            this.data = roots
        }

    @Test
    fun root_request_returns_first_root_real_path() {
        val s = share(listOf(photosRoot))
        assertEquals("/storage/emulated/0/photos", ShareManager.resolveVirtualPath(s, "/"))
        assertEquals("/storage/emulated/0/photos", ShareManager.resolveVirtualPath(s, ""))
    }

    @Test
    fun child_file_resolves_under_root() {
        val s = share(listOf(photosRoot))
        assertEquals(
            "/storage/emulated/0/photos/IMG_1.jpg",
            ShareManager.resolveVirtualPath(s, "photos/IMG_1.jpg"),
        )
    }

    @Test
    fun nested_directory_resolves_under_root() {
        val s = share(listOf(photosRoot))
        assertEquals(
            "/storage/emulated/0/photos/2026/vacation.jpg",
            ShareManager.resolveVirtualPath(s, "photos/2026/vacation.jpg"),
        )
    }

    @Test
    fun multiple_roots_resolve_independently() {
        val s = share(listOf(photosRoot, ShareRoot(virtualPath = "docs/", realPath = "/storage/emulated/0/Documents", isDir = true)))
        assertEquals("/storage/emulated/0/Documents/report.pdf", ShareManager.resolveVirtualPath(s, "docs/report.pdf"))
        assertEquals("/storage/emulated/0/photos/a.jpg", ShareManager.resolveVirtualPath(s, "photos/a.jpg"))
    }

    @Test
    fun unknown_top_level_root_is_rejected() {
        val s = share(listOf(photosRoot))
        assertNull(ShareManager.resolveVirtualPath(s, "videos/movie.mp4"))
    }

    @Test
    fun traversal_via_double_dot_is_blocked() {
        val s = share(listOf(photosRoot))
        assertNull(ShareManager.resolveVirtualPath(s, "photos/../secret.txt"))
    }

    @Test
    fun deep_traversal_escaping_root_is_blocked() {
        val s = share(listOf(photosRoot))
        assertNull(ShareManager.resolveVirtualPath(s, "photos/../../../etc/passwd"))
    }

    @Test
    fun single_dot_segments_are_collapsed() {
        val s = share(listOf(photosRoot))
        assertEquals(
            "/storage/emulated/0/photos/a.jpg",
            ShareManager.resolveVirtualPath(s, "photos/./a.jpg"),
        )
    }

    @Test
    fun expired_share_rejects_all_requests() {
        val expiredAt = Instant.fromEpochMilliseconds(0) // 1970-01-01
        val s = share(listOf(photosRoot), expiresAt = expiredAt)
        assertNull(ShareManager.resolveVirtualPath(s, "/"))
        assertNull(ShareManager.resolveVirtualPath(s, "photos/IMG_1.jpg"))
    }
}
