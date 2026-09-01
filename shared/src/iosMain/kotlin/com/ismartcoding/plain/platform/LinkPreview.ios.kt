package com.ismartcoding.plain.platform

import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.toNSData
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.UIKit.UIImage

@OptIn(ExperimentalForeignApi::class)
actual suspend fun getImageDimensions(data: ByteArray): Pair<Int, Int> {
    val uiImage = UIImage.imageWithData(data.toNSData())
    val width = uiImage?.size?.useContents { width.toInt() } ?: 0
    val height = uiImage?.size?.useContents { height.toInt() } ?: 0
    return width to height
}

actual suspend fun importImageBytesToFid(data: ByteArray, mimeType: String): String? {
    return try {
        val tempPath = createTempFilePath("linkpreview")
        if (!writeBytesToPath(tempPath, data)) return null
        importAppFile(tempPath, "", mimeType, deleteSrc = true)?.let { "fid:$it" }
    } catch (e: Exception) {
        LogCat.e("importImageBytesToFid: ${e.message}")
        null
    }
}