package com.ismartcoding.plain.helpers

import java.io.File

/**
 * Validates file paths before destructive operations to prevent accidental or
 * malicious deletion of critical system directories.
 */
object FilePathValidator {

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

    fun isSafeToDelete(path: String, allowedRoots: List<String> = emptyList()): Boolean {
        if (path.isBlank()) return false

        val file = File(path)
        if (!file.isAbsolute) return false

        val canonical = try {
            file.canonicalPath
        } catch (_: Exception) {
            return false
        }

        val parts = canonical.trimEnd('/').split('/').filter { it.isNotEmpty() }
        if (parts.size < 2) return false

        for (prefix in FORBIDDEN_PREFIXES) {
            if (canonical == prefix || canonical.startsWith("$prefix/")) return false
        }

        if (allowedRoots.isNotEmpty()) {
            val underAllowed = allowedRoots.any { root ->
                val canonicalRoot = try { File(root).canonicalPath } catch (_: Exception) { return@any false }
                canonical == canonicalRoot || canonical.startsWith("$canonicalRoot/")
            }
            if (!underAllowed) return false
        }

        return true
    }

    fun requireAllSafe(paths: List<String>, allowedRoots: List<String> = emptyList()) {
        paths.forEach { path ->
            require(isSafeToDelete(path, allowedRoots)) {
                "Path is not allowed for deletion: $path"
            }
        }
    }
}