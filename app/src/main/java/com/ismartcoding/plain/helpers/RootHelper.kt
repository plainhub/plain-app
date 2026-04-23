package com.ismartcoding.plain.helpers

import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.features.file.DFile
import kotlin.time.Instant
import java.io.DataOutputStream

/**
 * Helper for performing file operations on rooted devices.
 *
 * Key challenges on Android 11+:
 * - /storage/emulated/0/Android/data is blocked by FUSE even with MANAGE_EXTERNAL_STORAGE.
 * - Root shells access the real underlying path (/data/media/0/...) instead of the FUSE path.
 * - su execution via stdin is more reliable than `su -c` argument passing.
 */
object RootHelper {
    @Volatile
    private var rootAvailable: Boolean? = null

    // Matches /storage/emulated/<userId>/...
    private val EMULATED_RE = Regex("^/storage/emulated/(\\d+)(.*?)/?$")
    // Matches /data/media/<userId>/...
    private val MEDIA_RE = Regex("^/data/media/(\\d+)(.*?)/?$")

    fun isRooted(): Boolean {
        return rootAvailable ?: synchronized(this) {
            rootAvailable ?: run {
                val result = checkRoot()
                rootAvailable = result
                result
            }
        }
    }

    private fun checkRoot(): Boolean {
        return try {
            exec("id").contains("uid=0")
        } catch (e: Exception) {
            LogCat.d("RootHelper: root check failed: ${e.message}")
            false
        }
    }

    /**
     * Executes [command] in a root shell via stdin.
     * Uses the stdin approach (`su` + write to stdin) which works with all major
     * root implementations (Magisk, KernelSU, APatch) and avoids argument-quoting issues.
     * Reads stdout in a separate thread to prevent pipe-buffer deadlock.
     */
    private fun exec(command: String): String {
        var proc: Process? = null
        return try {
            proc = Runtime.getRuntime().exec("su")
            var output = ""
            // Read stdout in a background thread to avoid deadlock when the pipe buffer fills.
            val readerThread = Thread {
                output = proc.inputStream.bufferedReader().readText()
            }
            readerThread.start()
            val stdin = DataOutputStream(proc.outputStream)
            stdin.writeBytes("$command\nexit\n")
            stdin.flush()
            stdin.close()
            readerThread.join(15_000)
            output
        } catch (e: Exception) {
            LogCat.d("RootHelper: exec failed: ${e.message}")
            ""
        } finally {
            try { proc?.destroy() } catch (_: Exception) {}
        }
    }

    /**
     * Translates a FUSE path (/storage/emulated/N/...) to the real underlying path
     * (/data/media/N/...) that root shells can access on Android 11+.
     */
    private fun toRealPath(path: String): String {
        val m = EMULATED_RE.matchEntire(path.trimEnd('/')) ?: return path
        return "/data/media/${m.groupValues[1]}${m.groupValues[2]}"
    }

    /**
     * Translates a real path (/data/media/N/...) back to the FUSE path
     * (/storage/emulated/N/...) used by the rest of the app.
     */
    private fun fromRealPath(path: String): String {
        val m = MEDIA_RE.matchEntire(path) ?: return path
        return "/storage/emulated/${m.groupValues[1]}${m.groupValues[2]}"
    }

    /**
     * Lists files in [dir] using a root shell.
     * Translates FUSE paths to real paths for the shell command, then maps results back.
     */
    fun listFiles(dir: String, showHidden: Boolean): List<DFile> {
        val realDir = toRealPath(dir)
        // Escape double-quotes in path for the shell
        val safeDir = realDir.replace("\"", "\\\"")
        // stat -c: %n=path, %F=file type, %s=size, %Y=mtime epoch seconds
        // Use double-quotes around the path so word-splitting is safe.
        // The /* and /.[!.]* globs are outside the quotes so the shell expands them.
        // 2>/dev/null silences "no match" errors when globs don't expand.
        val cmd = buildString {
            append("stat -c '%n|%F|%s|%Y' \"$safeDir\"/* 2>/dev/null")
            if (showHidden) {
                // .[!.]* matches .hidden but not . or ..
                // ..?* matches ..hidden-style names
                append("; stat -c '%n|%F|%s|%Y' \"$safeDir\"/.[!.]* \"$safeDir\"/..?* 2>/dev/null")
            }
        }
        val output = exec(cmd)
        if (output.isBlank()) return emptyList()

        val seen = mutableSetOf<String>()
        return output.lines()
            .filter { it.contains('|') }
            .mapNotNull { line ->
                val parts = line.split("|", limit = 4)
                if (parts.size < 4) return@mapNotNull null
                val rawPath = parts[0].trim()
                val name = rawPath.substringAfterLast('/')
                if (name.isEmpty() || name == "." || name == "..") return@mapNotNull null
                if (!seen.add(rawPath)) return@mapNotNull null // deduplicate
                val typeName = parts[1]
                val size = parts[2].toLongOrNull() ?: 0L
                val epochSeconds = parts[3].trim().toLongOrNull() ?: 0L
                // Map real path back to FUSE path for app-internal use
                val displayPath = fromRealPath(rawPath)
                val isDir = typeName.contains("directory")
                DFile(
                    name = name,
                    path = displayPath,
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
