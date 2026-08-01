@file:OptIn(ExperimentalForeignApi::class)

package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.extensions.getFilenameExtension
import com.ismartcoding.plain.lib.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager
import platform.Foundation.writeToFile

actual fun saveFeedImage(feedId: String, imageUrl: String, bytes: ByteArray): String? {
    val dir = appDir() + "/feeds/${feedId}"
    NSFileManager.defaultManager.createDirectoryAtPath(
        dir, withIntermediateDirectories = true, attributes = null, error = null,
    )
    var path = "$dir/main-${sha1(imageUrl.encodeToByteArray())}"
    val extension = imageUrl.getFilenameExtension()
    if (extension.isNotEmpty()) {
        path += ".$extension"
    }
    bytes.toNSData().writeToFile(path, atomically = true)
    return path
}
