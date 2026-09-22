package com.ismartcoding.plain.ui.models

import java.io.File
import kotlin.test.Test
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * Locks the queue-VM state sync contract (2026-09-23). AudioQueueViewModel's
 * derived fields (everything refreshWindow computes from AudioQueueManager)
 * must be assigned ONLY inside refreshWindow. clearAsync used to hand-reset
 * the fields one by one, missed activePlaylistId (added later), and left the
 * playlist detail page thinking its playlist was still the active source —
 * Play All then only tried pause/resume and did nothing. Mutating functions
 * must sync through refreshWindow instead of re-deriving by hand.
 */
class QueueStateSyncGuardTest {

    private val vmPath =
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/ui/models/AudioQueueViewModel.kt"

    /** Fields derived from AudioQueueManager state; refreshWindow owns them. */
    private val derivedFields = listOf(
        "queueItems", "queueCount", "noMore", "canReorder", "activePlaylistId", "queuedPaths",
    )

    /**
     * Sanctioned "fn:field" hand assignments. reorder() optimistically moves an
     * item inside the already-loaded window; no other derived field changes and
     * a refreshWindow round-trip per drag step would hammer the DB.
     */
    private val allowedAssignments = setOf("reorder:queueItems")

    private fun vmSource(): String {
        var dir = File(System.getProperty("user.dir")!!).absoluteFile
        for (i in 0 until 4) {
            val f = File(dir, vmPath)
            if (f.isFile) return f.readText()
            dir = dir.parentFile ?: break
        }
        fail("source not found: $vmPath (from ${System.getProperty("user.dir")})")
    }

    /** All top-level fun declarations with their brace-matched body ranges. */
    private fun functions(source: String): List<Triple<String, IntRange, String>> {
        val out = mutableListOf<Triple<String, IntRange, String>>()
        Regex("fun (\\w+)\\(").findAll(source).forEach { m ->
            val open = source.indexOf('{', m.range.last)
            if (open < 0) return@forEach
            var depth = 0
            for (i in open until source.length) {
                when (source[i]) {
                    '{' -> depth++
                    '}' -> {
                        depth--
                        if (depth == 0) {
                            out.add(Triple(m.groupValues[1], m.range.first..i, source.substring(open, i + 1)))
                            return@forEach
                        }
                    }
                }
            }
        }
        return out
    }

    @Test
    fun `derived queue fields are assigned only inside refreshWindow`() {
        val source = vmSource()
        val fns = functions(source)
        val violations = mutableListOf<String>()
        derivedFields.forEach { field ->
            val sites = Regex("$field\\.value\\s*=").findAll(source).map { it.range.first }.toList()
            if (sites.isEmpty()) violations.add("$field: no assignment found anywhere (field removed?)")
            sites.forEach { pos ->
                val owner = fns.firstOrNull { pos in it.second }?.first ?: "<none>"
                if (owner != "refreshWindow" && "$owner:$field" !in allowedAssignments) {
                    violations.add("$field assigned in $owner()")
                }
            }
        }
        assertTrue(violations.isEmpty(), "Queue state must sync via refreshWindow():\n${violations.joinToString("\n")}")
    }

    @Test
    fun `refreshWindow derives every known state field - list stays synced`() {
        val body = functions(vmSource()).first { it.first == "refreshWindow" }.third
        val missing = derivedFields.filter { !body.contains("$it.value =") }
        assertTrue(missing.isEmpty(), "refreshWindow no longer derives: ${missing.joinToString()} — update QueueStateSyncGuardTest.derivedFields")
    }

    @Test
    fun `clear and playSingle sync through refreshWindow`() {
        val fns = functions(vmSource())
        listOf("clearAsync", "playSingleAsync").forEach { name ->
            val fn = fns.firstOrNull { it.first == name } ?: fail("$name() missing from AudioQueueViewModel")
            assertTrue(
                fn.third.contains("refreshWindow()"),
                "$name() must sync derived state via refreshWindow(), not hand-set fields",
            )
        }
    }
}
