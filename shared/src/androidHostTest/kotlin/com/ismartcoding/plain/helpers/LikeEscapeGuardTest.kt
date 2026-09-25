package com.ismartcoding.plain.helpers

import java.io.File
import kotlin.test.Test
import kotlin.test.fail

/**
 * Locks the LIKE-escaping convention (2026-09-25) at the source level:
 *
 * 1. Every LIKE whose pattern binds a placeholder (`LIKE ?` / `LIKE :x` /
 *    `LIKE '%' || ? ...`) must carry `ESCAPE` on the same source line — a
 *    missing ESCAPE turns escapeLike's backslashes into literal characters
 *    and the search silently matches nothing.
 * 2. `escapeLike(` may only appear in the allowlisted files below. A new
 *    call site means a new manual LIKE pairing — adding it to the allowlist
 *    is a conscious act that shows up in review.
 * 3. No second escaping implementation: `replace("%", "\\%")`-style ad-hoc
 *    escapes outside ContentWhere.kt are forbidden (ClipboardHelper's old
 *    likePattern regression).
 *
 * LIKE with a constant wildcard pattern (Doc MIME probe `LIKE ?` bound to
 * "text/%") is exempt — the arg is code, not user input.
 *
 * Scan scope: shared/src, shared-lib/src, room-db/src, app/src — production
 * source sets only (test sources are excluded; the guard itself contains the
 * patterns it forbids).
 */
class LikeEscapeGuardTest {

    /** Files allowed to call escapeLike (the pairing half outside ContentWhere). */
    private val escapeLikeAllowlist = setOf(
        "shared-lib/src/commonMain/kotlin/com/ismartcoding/plain/helpers/ContentWhere.kt",
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/httpserver/mainschemas/ChatMessageGraphQL.kt",
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/httpserver/mainschemas/AppFileGraphQL.kt",
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/features/audio/AudioPlayHistoryManager.kt",
        "shared/src/commonMain/kotlin/com/ismartcoding/plain/features/audio/AudioPlaylistManager.kt",
        "shared/src/androidMain/kotlin/com/ismartcoding/plain/docs/DocMediaStoreHelper.kt",
    )

    /** LIKE-with-placeholder lines exempt because the pattern arg is a code constant. */
    private val constantPatternMarkers = listOf("MIME_TYPE} LIKE ?")

    private val placeholderLike = Regex("""LIKE\s+[:?]|LIKE\s*'[^']*'\s*\|\|\s*\?""")
    private val adHocEscaping = Regex("""replace\("[%_]"\s*,\s*"[\\]{2}""")

    private fun repoRoot(): File {
        var dir = File(System.getProperty("user.dir")).absoluteFile
        repeat(6) {
            if (File(dir, "shared/src").isDirectory && File(dir, "room-db/src").isDirectory) return dir
            dir = dir.parentFile ?: return@repeat
        }
        fail("repo root with shared/src and room-db/src not found (cwd=${System.getProperty("user.dir")})")
    }

    private fun productionSources(): List<File> {
        val root = repoRoot()
        val srcRoots = listOf(
            File(root, "shared/src"),
            File(root, "shared-lib/src"),
            File(root, "room-db/src"),
            File(root, "app/src"),
        ).filter { it.isDirectory }
        val testDirs = setOf("commonTest", "androidHostTest", "test", "androidTest", "iosTest")
        return srcRoots.flatMap { src ->
            src.walkTopDown()
                .filter { it.isFile && it.extension == "kt" }
                .filter { file -> file.relativeTo(src).invariantSeparatorsPath.split('/').none { it in testDirs } }
                .toList()
        }
    }

    @Test
    fun placeholderLikesCarryEscape() {
        productionSources().forEach { file ->
            file.readLines().forEachIndexed { i, line ->
                if (!placeholderLike.containsMatchIn(line)) return@forEachIndexed
                if (constantPatternMarkers.any { line.contains(it) }) return@forEachIndexed
                if (!line.contains("ESCAPE")) {
                    fail("LIKE with a bound placeholder must pair ESCAPE on the same line — ${file.path}:${i + 1}: ${line.trim()}")
                }
            }
        }
    }

    @Test
    fun escapeLikeStaysInAllowlistedFiles() {
        productionSources().forEach { file ->
            val relative = file.path.removePrefix(repoRoot().path + File.separator)
            if (file.readText().contains("escapeLike(") && relative !in escapeLikeAllowlist) {
                fail("escapeLike( used outside the allowlist — add the file consciously (it means a new manual LIKE pairing): $relative")
            }
        }
        // And every allowlisted entry must still exist — no stale entries.
        val present = productionSources().map { it.path.removePrefix(repoRoot().path + File.separator) }.toSet()
        escapeLikeAllowlist.forEach { entry ->
            if (entry !in present) fail("escapeLike allowlist entry no longer exists: $entry")
        }
    }

    @Test
    fun noSecondEscapingImplementation() {
        productionSources().forEach { file ->
            val relative = file.path.removePrefix(repoRoot().path + File.separator)
            if (relative.endsWith("ContentWhere.kt")) return@forEach
            file.readLines().forEachIndexed { i, line ->
                if (adHocEscaping.containsMatchIn(line)) {
                    fail("ad-hoc LIKE escaping outside ContentWhere — use escapeLike/addLike: ${file.path}:${i + 1}: ${line.trim()}")
                }
            }
        }
    }
}
