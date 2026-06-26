package com.ismartcoding.plain.lib.logcat

import java.io.File

actual class DiskLogStrategy actual constructor() : LogStrategy {
    actual override fun log(priority: Int, tag: String?, message: String) {
        val folder = LogCat.logFolder()
        if (folder.isEmpty()) return
        val dir = File(folder)
        if (!dir.exists()) dir.mkdirs()
        val file = File(dir, "latest.log")
        file.appendText(message + "\n")
        if (file.length() > MAX_BYTES) {
            val backup = File(dir, "latest.log.bak")
            if (backup.exists()) backup.delete()
            file.renameTo(backup)
        }
    }

    private companion object {
        const val MAX_BYTES = 25L * 1024 * 1024
    }
}