package com.ismartcoding.plain.lib.logcat

import com.ismartcoding.plain.platform.appendLine
import com.ismartcoding.plain.platform.deleteFileIfExists
import com.ismartcoding.plain.platform.ensureDir
import com.ismartcoding.plain.platform.renameFile

class DiskLogStrategy : LogStrategy {
    override fun log(priority: Int, tag: String?, message: String) {
        val folder = LogCat.logFolder()
        if (folder.isEmpty()) return
        ensureDir(folder)
        val filePath = "$folder/latest.log"
        val size = appendLine(filePath, message + "\n")
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
