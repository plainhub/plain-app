package com.ismartcoding.plain.ui.models

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

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
    fun `media UI delegates actions without preemptive reference cleanup`() {
        val body = functionBody(source(baseMediaVmPath), "trashItems")
        assertTrue(body.contains("trashMedia(dataType, ids)"))
        assertTrue(!body.contains("deleteTagRelationByKeys"))
        assertTrue(!body.contains("onTrashed"))
        assertTrue(!source(audioVmPath).contains("override suspend fun onTrashed"))
    }

    @Test
    fun `native media actions call the shared Rust coordinator`() {
        val bridge = source("shared/src/androidMain/kotlin/com/ismartcoding/plain/platform/MediaStore.android.kt")
        for (name in listOf("trashMedia","restoreMedia","deleteMedia","moveMedia")) {
            assertTrue(functionBody(bridge,name).contains("MediaActionHelper.run"), "$name must route through Rust")
        }
        val helper = source("shared/src/commonMain/kotlin/com/ismartcoding/plain/features/mediaactions/MediaActionHelper.kt")
        assertTrue(helper.contains("mediaHostAction"))
        assertTrue(helper.contains("failedIds"))
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
