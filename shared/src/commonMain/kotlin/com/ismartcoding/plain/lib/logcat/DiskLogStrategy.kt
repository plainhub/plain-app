package com.ismartcoding.plain.lib.logcat

class DiskLogStrategy : LogStrategy {
    override fun log(priority: Int, tag: String?, message: String) {
        val folder = LogCat.logFolder()
        if (folder.isEmpty()) return
        ensureLogDir(folder)
        val filePath = "$folder/latest.log"
        val size = appendLogLine(filePath, message + "\n")
        if (size > MAX_BYTES) {
            val backupPath = "$folder/latest.log.bak"
            deleteFileIfExists(backupPath)
            renameFile(filePath, backupPath)
        }
    }

    companion object {
        private const val MAX_BYTES = 25L * 1024 * 1024
    }
}

internal expect fun ensureLogDir(path: String)

internal expect fun appendLogLine(path: String, line: String): Long

internal expect fun deleteFileIfExists(path: String)

internal expect fun renameFile(from: String, to: String)
