@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.Foundation.NSLog
import platform.posix.fclose
import platform.posix.fflush
import platform.posix.fopen
import platform.posix.fwrite

actual fun ensureDir(path: String) {
    val fm = NSFileManager.defaultManager
    if (!fm.fileExistsAtPath(path)) {
        fm.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)
    }
}

actual fun appendLine(path: String, line: String): Long {
    val fm = NSFileManager.defaultManager
    if (!fm.fileExistsAtPath(path)) {
        fm.createFileAtPath(path, null, null)
    }
    val fp = fopen(path, "a")
    if (fp == null) {
        NSLog("FileOps: fopen failed for %@", path)
    } else {
        try {
            val data = line.encodeToByteArray()
            data.usePinned { pinned ->
                val written = fwrite(pinned.addressOf(0), 1.toULong(), data.size.toULong(), fp)
                if (written < data.size.toULong()) {
                    NSLog("FileOps: short write for %@", path)
                }
            }
            fflush(fp)
        } finally {
            fclose(fp)
        }
    }
    return (fm.attributesOfItemAtPath(path, null)?.get(NSFileSize) as? Long) ?: 0L
}

actual fun deleteFileIfExists(path: String) {
    val fm = NSFileManager.defaultManager
    if (fm.fileExistsAtPath(path)) {
        fm.removeItemAtPath(path, null)
    }
}

actual fun renameFile(from: String, to: String) {
    NSFileManager.defaultManager.moveItemAtPath(from, toPath = to, error = null)
}
