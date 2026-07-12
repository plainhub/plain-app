package com.ismartcoding.plain.platform

import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageType
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSFileManager

@OptIn(ExperimentalForeignApi::class)
actual fun deleteFileAt(path: String) {
    NSFileManager.defaultManager.removeItemAtPath(path, null)
}

actual fun getFileId(path: String): String = ""

actual fun createLongTextFile(text: String): DMessageContent {
    return DMessageContent(
        DMessageType.FILES.value,
        DMessageFiles(emptyList())
    )
}

actual fun saveFileToDownloads(path: String, fileName: String): String = ""

actual fun fileToUriString(path: String): String = path

actual fun getFileIconPath(extension: String): String = ""

@OptIn(ExperimentalForeignApi::class)
actual fun fileExists(path: String): Boolean =
    NSFileManager.defaultManager.fileExistsAtPath(path)
