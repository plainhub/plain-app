package com.ismartcoding.plain.features.audio

import com.ismartcoding.plain.preferences.*
import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Locks the playAudio queue-order contract (2026-09-27, T58): playing a track
 * must NOT mutate or reorder the playback queue. playAudio used to append the
 * track to the manual queue unconditionally (enqueue moves an existing entry
 * to the tail), so every desktop play jumped the clicked track to the end of
 * the refetched audioQueue list. Now playAudio only marks the current track;
 * callers that need the track queued combine addAudiosToQueue/enqueue with the
 * play themselves (enqueueAndPlayAsync, or two mutations in one document from
 * the desktop). The `playAudio` half of this now lives in Rust, where the
 * resolver is, and is guarded there; the client-side wiring below has no
 * host-testable seam (platform AppDatabase) so it stays a source scan.
 */
class PlayAudioQueueOrderGuardTest {

    private val vmPath =
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/models/AudioQueueViewModel.kt"
    private val searchPath =
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/page/search/GlobalSearchPage.kt"

    private fun source(relPath: String): String {
        var dir = File(System.getProperty("user.dir")!!).absoluteFile
        for (i in 0 until 4) {
            val f = File(dir, relPath)
            if (f.isFile) return f.readText()
            dir = dir.parentFile ?: break
        }
        fail("source not found: $relPath (from ${System.getProperty("user.dir")})")
    }

    /** Brace-matched body of top-level function [name], or the whole file when absent. */
    private fun functionBody(source: String, name: String): String? {
        val m = Regex("fun $name\\(").find(source) ?: return null
        val open = source.indexOf('{', m.range.last)
        if (open < 0) return null
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
        return null
    }

    @Test
    fun queueTapPlaysInPlaceWithoutEnqueue() {
        val body = functionBody(source(vmPath), "playAsync")
            ?: fail("playAsync not found in $vmPath")
        assertTrue(
            "audioJustPlay(" in body,
            "queue-tap playAsync must start playback",
        )
        assertTrue(
            "enqueue(" !in body,
            "queue-tap playAsync plays an item that is already in the queue; enqueueing it would move it to the tail and reorder the queue",
        )
    }

    @Test
    fun outOfQueuePlaysCombineEnqueueWithPlay() {
        val vm = source(vmPath)
        val combiner = functionBody(vm, "enqueueAndPlayAsync")
            ?: fail("enqueueAndPlayAsync not found in $vmPath")
        assertTrue(
            "AudioQueueManager.enqueue(" in combiner && "audioJustPlay(" in combiner,
            "enqueueAndPlayAsync is the sanctioned enqueue+play combiner for tracks outside the queue",
        )
        val search = source(searchPath)
        assertTrue(
            "enqueueAndPlayAsync(" in search && ".playAsync(" !in search,
            "global search plays a track that may be outside the queue; it must go through enqueueAndPlayAsync, not the in-place playAsync",
        )
    }
}
