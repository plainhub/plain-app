@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.withIO
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL

actual suspend fun stagePickedFile(uri: String, path: String) = withIO {
    val source = NSURL.URLWithString(uri)?.path ?: uri
    val manager = NSFileManager.defaultManager
    check(manager.createDirectoryAtPath(path.substringBeforeLast('/'), true, null, null)) { "Unable to create selected file directory" }
    check(manager.copyItemAtPath(source, path, null)) { "Unable to read selected file" }
}
