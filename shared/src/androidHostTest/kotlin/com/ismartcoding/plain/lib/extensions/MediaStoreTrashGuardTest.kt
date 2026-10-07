package com.ismartcoding.plain.lib.extensions

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.fail

/**
 * MediaStore owns the phone's trash, and a trashed-only read is a query
 * argument rather than a selection: the provider plan hands `trash` over
 * without building a clause, because MediaProvider parses a legacy selection
 * with a strict grammar that rejects `is_trashed`. These bundle builders are
 * the whole of that argument's plumbing — drop it from one and the trash view,
 * the drawer badge and every restore target quietly answer with the ordinary
 * library instead, with nothing left to notice.
 */
class MediaStoreTrashGuardTest {

    private fun source(relativePath: String): String {
        var dir = File(System.getProperty("user.dir")!!).absoluteFile
        for (i in 0 until 4) {
            val f = File(dir, relativePath)
            if (f.isFile) return f.readText()
            dir = dir.parentFile ?: break
        }
        fail("source not found: $relativePath (from ${System.getProperty("user.dir")})")
    }

    /** Body of the named top-level fun, brace-matched. Extension receivers allowed. */
    private fun functionBody(source: String, name: String): String {
        val m = Regex("fun [\\w.]*?$name\\(").find(source) ?: fail("fun $name() not found")
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

    private fun extension(vararg path: String): String =
        source("shared/src/androidMain/kotlin/com/ismartcoding/plain/${path.joinToString("/")}")

    private val contentResolver: String
        get() = extension("lib/extensions/ContentResolver.kt")

    @Test
    fun `every bundle read asks MediaStore for the trashed set`() {
        val names = Regex("fun [\\w.]*?(\\w+WithBundle)\\(").findAll(contentResolver)
            .map { it.groupValues[1] }
            .toList()
        assertEquals(3, names.size, "expected the three bundle reads, found $names")
        for (name in names) {
            val body = functionBody(contentResolver, name)
            assertTrue(
                body.contains("MediaStore.QUERY_ARG_MATCH_TRASHED") && body.contains("MediaStore.MATCH_ONLY"),
                "$name does not ask MediaStore for the trashed set (MATCH_ONLY, not MATCH_EXCLUDE); found: ${body.take(400)}",
            )
        }
    }

    /**
     * The NAS store moves files into a `.plain-trash` tree; the phone has never
     * created one. A path match for it in the phone's own reads is the defect
     * that let a whole-table trashed query through — it produced a clause, so
     * the destructive guard read the query as narrowed while it selected every
     * row the provider held.
     */
    @Test
    fun `the NAS trash tree never comes back to the phone`() {
        for (path in listOf(
            "lib/extensions/ContentResolver.kt",
            "features/mediaactions/NativeMediaActions.kt",
            "platform/MediaStore.android.kt",
        )) {
            assertFalse(
                extension(path).contains(".plain-trash"),
                "$path names the NAS trash tree; MediaStore owns the phone's trash",
            )
        }
    }
}