@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.lib.logcat

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.Foundation.NSFileManager
import platform.Foundation.NSFileSize
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite

internal actual fun ensureLogDir(path: String) {
    val fm = NSFileManager.defaultManager
    if (!fm.fileExistsAtPath(path)) {
        fm.createDirectoryAtPath(path, withIntermediateDirectories = true, attributes = null, error = null)
    }
}

internal actual fun appendLogLine(path: String, line: String): Long {
    val fm = NSFileManager.defaultManager
    if (!fm.fileExistsAtPath(path)) {
        fm.createFileAtPath(path, null, null)
    }
    val fp = fopen(path, "a")
    if (fp != null) {
        try {
            val data = line.encodeToByteArray()
            data.usePinned { pinned ->
                fwrite(pinned.addressOf(0), 1.toULong(), data.size.toULong(), fp)
            }
        } finally {
            fclose(fp)
        }
    }
    val attrs = fm.attributesOfItemAtPath(path, error = null)
    return (attrs?.get(NSFileSize) as? Long) ?: 0L
}

internal actual fun deleteFileIfExists(path: String) {
    val fm = NSFileManager.defaultManager
    if (fm.fileExistsAtPath(path)) {
        fm.removeItemAtPath(path, null)
    }
}

internal actual fun renameFile(from: String, to: String) {
    NSFileManager.defaultManager.moveItemAtPath(from, toPath = to, error = null)
}
