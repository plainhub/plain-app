package com.ismartcoding.plain.helpers

import java.io.File

/**
 * Validates file paths before destructive operations to prevent accidental or
 * malicious deletion of critical system directories.
 */
object FilePathValidator {

    /**
     * Paths (and their canonical forms) that must never be deleted, regardless
     * of what the caller provides.
     */
    private val FORBIDDEN_PREFIXES = listOf(
        "/system",
        "/proc",
        "/sys",
        "/dev",
        "/data/data",
        "/data/app",
        "/data/system",
        "/data/misc",
        "/vendor",
        "/product",
        "/apex",
        "/oem",
        "/odm",
    )

    /**
     * Returns true when [path] is safe to delete:
     * - Not blank
     * - Absolute and at least 2 path components deep (prevents "/" or "/sdcard")
     * - Does not traverse outside its declared root (canonical == declared)
     * - Does not start with a forbidden system prefix
     * - If [allowedRoots] is non-empty the path must be under one of them
     *
     * @param path        The raw path string from the caller.
     * @param allowedRoots Optional list of storage roots that paths must live under
     *                     (e.g. ["/sdcard", "/storage/emulated/0"]).
     *                     Pass an empty list to skip the root-containment check.
     */
    fun isSafeToDelete(path: String, allowedRoots: List<String> = emptyList()): Boolean {
        if (path.isBlank()) return false

        val file = File(path)
        if (!file.isAbsolute) return false

        // Resolve symlinks / ".." before any further checks
        val canonical = try {
            file.canonicalPath
        } catch (_: Exception) {
            return false
        }

        // Must have at least 2 path components, e.g. "/foo/bar"
        // This rejects "/" and single-component roots like "/sdcard" or "/storage"
        val parts = canonical.trimEnd('/').split('/').filter { it.isNotEmpty() }
        if (parts.size < 2) return false

        // Reject if path traversal changed the directory (e.g. "/../etc")
        // and canonical ends up under a forbidden area
        for (prefix in FORBIDDEN_PREFIXES) {
            if (canonical == prefix || canonical.startsWith("$prefix/")) return false
        }

        // If specific allowed roots are provided, the path must be under one of them
        if (allowedRoots.isNotEmpty()) {
            val underAllowed = allowedRoots.any { root ->
                val canonicalRoot = try { File(root).canonicalPath } catch (_: Exception) { return@any false }
                canonical == canonicalRoot || canonical.startsWith("$canonicalRoot/")
            }
            if (!underAllowed) return false
        }

        return true
    }

    /**
     * Throws [IllegalArgumentException] if any path in [paths] is not safe to delete.
     */
    fun requireAllSafe(paths: List<String>, allowedRoots: List<String> = emptyList()) {
        paths.forEach { path ->
            require(isSafeToDelete(path, allowedRoots)) {
                "Path is not allowed for deletion: $path"
            }
        }
    }
}
