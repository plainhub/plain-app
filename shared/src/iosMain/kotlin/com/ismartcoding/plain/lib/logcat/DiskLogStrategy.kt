package com.ismartcoding.plain.lib.logcat

import platform.Foundation.NSFileHandle
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize

actual class DiskLogStrategy actual constructor() : LogStrategy {
    actual override fun log(priority: Int, tag: String?, message: String) {
        val folder = LogCat.logFolder()
        val fm = NSFileManager.defaultManager
        if (!fm.fileExistsAtPath(folder)) {
            fm.createDirectoryAtPath(folder, withIntermediateDirectories = true, attributes = null, error = null)
        }
        val filePath = "$folder/latest.log"
        if (!fm.fileExistsAtPath(filePath)) {
            fm.createFileAtPath(filePath, null, null)
        }
        val handle = NSFileHandle.fileHandleForWritingAtPath(filePath) ?: return
        try {
            handle.seekToEndOfFile()
            val data = (message + "\n").encodeToByteArray().toNSData()
            handle.writeData(data)
        } finally {
            handle.closeFile()
        }
        val attrs = fm.attributesOfItemAtPath(filePath, error = null)
        val size = (attrs?.get(NSFileSize) as? Long) ?: 0L
        if (size > MAX_BYTES) {
            val backupPath = "$folder/latest.log.bak"
            if (fm.fileExistsAtPath(backupPath)) {
                fm.removeItemAtPath(backupPath, null)
            }
            fm.moveItemAtPath(filePath, toPath = backupPath, error = null)
        }
    }

    private companion object {
        const val MAX_BYTES = 25L * 1024 * 1024
    }
}