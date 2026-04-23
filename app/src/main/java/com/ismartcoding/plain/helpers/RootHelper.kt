package com.ismartcoding.plain.helpers

import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.features.file.DFile
import kotlin.time.Instant
import java.util.concurrent.TimeUnit

/**
 * Helper for performing file operations on rooted devices.
 * On Android 11+, directories like Android/data are inaccessible via the normal File API
 * even with MANAGE_EXTERNAL_STORAGE. On rooted devices, we fall back to su shell commands.
 */
object RootHelper {
    @Volatile
    private var rootAvailable: Boolean? = null

    fun isRooted(): Boolean {
        return rootAvailable ?: run {
            val result = checkRoot()
            rootAvailable = result
            result
        }
    }

    private fun checkRoot(): Boolean {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val exited = proc.waitFor(3, TimeUnit.SECONDS)
            if (!exited) {
                proc.destroy()
                return false
            }
            val output = proc.inputStream.bufferedReader().readText()
            proc.errorStream.bufferedReader().readText() // drain stderr
            output.contains("uid=0")
        } catch (e: Exception) {
            LogCat.d("RootHelper: root check failed: ${e.message}")
            false
        }
    }

    private fun exec(command: String): String {
        return try {
            val proc = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
            val exited = proc.waitFor(15, TimeUnit.SECONDS)
            if (!exited) {
                proc.destroy()
                return ""
            }
            val output = proc.inputStream.bufferedReader().readText()
            proc.errorStream.bufferedReader().readText() // drain stderr
            output
        } catch (e: Exception) {
            LogCat.d("RootHelper: exec failed: ${e.message}")
            ""
        }
    }

    /**
     * Lists files in [dir] using root shell when normal File.listFiles() is blocked
     * (e.g. Android/data on Android 11+).
     * Uses stat to get type, size, and mtime in one command.
     * Children count is not populated to avoid extra subprocess calls.
     */
    fun listFiles(dir: String, showHidden: Boolean): List<DFile> {
        val safeDir = dir.replace("'", "\\'")
        // stat -c '%n|%F|%s|%Y': full path | type | size | epoch seconds
        // Non-hidden files: glob '*' (excludes dot-files)
        // Hidden files: additionally glob '.?*' (excludes '.' and '..')
        val cmd = if (showHidden) {
            "stat -c '%n|%F|%s|%Y' '$safeDir'/* '$safeDir'/.?* 2>/dev/null"
        } else {
            "stat -c '%n|%F|%s|%Y' '$safeDir'/* 2>/dev/null"
        }
        val output = exec(cmd)
        if (output.isBlank()) return emptyList()

        return output.lines()
            .filter { it.contains('|') }
            .mapNotNull { line ->
                val parts = line.split("|", limit = 4)
                if (parts.size < 4) return@mapNotNull null
                val fullPath = parts[0].trim()
                val typeName = parts[1]
                val size = parts[2].toLongOrNull() ?: 0L
                val epochSeconds = parts[3].trim().toLongOrNull() ?: 0L
                val name = fullPath.substringAfterLast('/')
                if (name.isEmpty() || name == "." || name == "..") return@mapNotNull null
                val isDir = typeName.contains("directory")
                DFile(
                    name = name,
                    path = fullPath,
                    permission = "",
                    createdAt = null,
                    updatedAt = Instant.fromEpochMilliseconds(epochSeconds * 1000L),
                    size = if (isDir) 0L else size,
                    isDir = isDir,
                    children = 0,
                )
            }
    }
}
