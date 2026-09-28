package com.ismartcoding.plain.helpers

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
        if (!path.startsWith("/")) return false

        val normalized = normalizePath(path)

        val parts = normalized.trimEnd('/').split('/').filter { it.isNotEmpty() }
        if (parts.size < 2) return false

        for (prefix in FORBIDDEN_PREFIXES) {
            if (normalized == prefix || normalized.startsWith("$prefix/")) return false
        }

        if (allowedRoots.isNotEmpty()) {
            val underAllowed = allowedRoots.any { root ->
                val normalizedRoot = normalizePath(root)
                normalized == normalizedRoot || normalized.startsWith("$normalizedRoot/")
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

    private fun normalizePath(path: String): String {
        val parts = path.split('/').filter { it.isNotEmpty() }
        val stack = ArrayDeque<String>()
        for (part in parts) {
            when (part) {
                "." -> { /* skip */ }
                ".." -> { if (stack.isNotEmpty()) stack.removeLast() }
                else -> stack.addLast(part)
            }
        }
        return "/" + stack.joinToString("/")
    }
}
