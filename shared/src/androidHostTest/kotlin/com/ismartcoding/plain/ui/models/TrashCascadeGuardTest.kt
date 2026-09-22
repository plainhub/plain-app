package com.ismartcoding.plain.ui.models

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Locks the trash-cascade contract (2026-09-23). App-UI trash (trashItems) used
 * to skip the path cleanup that the server flow (MediaGraphQL.trashMediaItems)
 * performs, so audios trashed in-app kept their playlist rows and still showed
 * on the playlist detail page. Paths must be resolved BEFORE trashMedia — once
 * IS_TRASHED is set, default MediaStore queries hide the rows and the paths are
 * unrecoverable.
 */
class TrashCascadeGuardTest {

    private fun source(relativePath: String): String {
        var dir = File(System.getProperty("user.dir")!!).absoluteFile
        for (i in 0 until 4) {
            val f = File(dir, relativePath)
            if (f.isFile) return f.readText()
            dir = dir.parentFile ?: break
        }
        fail("source not found: $relativePath (from ${System.getProperty("user.dir")})")
    }

    /** Body of the named top-level fun, brace-matched. */
    private fun functionBody(source: String, name: String): String {
        val m = Regex("fun $name\\(").find(source) ?: fail("fun $name() not found")
        val open = source.indexOf('{', m.range.last)
        var depth = 0
        for (i in open until source.length) {
            when (source[i]) {
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return source.substring(open, i + 1)
                }
            }
        }
        fail("fun $name() body not brace-matched")
    }

    @Test
    fun `trashItems resolves paths before trashing and calls the cascade hook`() {
        val body = functionBody(source(baseMediaVmPath), "trashItems")
        val resolve = body.indexOf("getMediaPathsByIds(dataType, ids)")
        val trash = body.indexOf("trashMedia(dataType, ids)")
        assertTrue(resolve >= 0, "trashItems must resolve paths via getMediaPathsByIds before trashing")
        assertTrue(trash > resolve, "paths must be resolved BEFORE trashMedia (trashed rows hide from default queries)")
        assertTrue(body.contains("onTrashed(paths)"), "trashItems must hand the paths to the onTrashed cascade hook")
    }

    @Test
    fun `audio trash cascades to queue, history and playlists like the server flow`() {
        val override = functionBody(source(audioVmPath), "onTrashed")
        assertTrue(
            override.contains("AudioQueueManager.removePaths(paths)"),
            "AudioViewModel.onTrashed must cascade via AudioQueueManager.removePaths",
        )
        val cascade = functionBody(source(queueManagerPath), "removePaths")
        listOf("queueDao.deleteByPaths", "AudioPlayHistoryManager.removePaths", "AudioPlaylistManager.removePaths")
            .forEach { call ->
                assertTrue(cascade.contains(call), "AudioQueueManager.removePaths must keep cascading: $call")
            }
    }

    @Test
    fun `playlist detail page prunes trashed rows on load`() {
        val page = source(playlistDetailPagePath)
        assertTrue(
            page.contains("list.pruneTrashedRows(playlistId)"),
            "PlaylistDetailPage reload must route rows through pruneTrashedRows so trashed items never show",
        )
    }

    private companion object {
        const val baseMediaVmPath =
            "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/models/BaseMediaViewModel.kt"
        const val audioVmPath =
            "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/models/AudioViewModel.kt"
        const val queueManagerPath =
            "shared/src/commonMain/kotlin/com/ismartcoding/plain/features/audio/AudioQueueManager.kt"
        const val playlistDetailPagePath =
            "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/playlist/PlaylistDetailPage.kt"
    }
}
