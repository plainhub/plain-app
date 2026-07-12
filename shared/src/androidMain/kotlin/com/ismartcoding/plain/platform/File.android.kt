package com.ismartcoding.plain.platform

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.db.DMessageContent
import com.ismartcoding.plain.db.DMessageFile
import com.ismartcoding.plain.db.DMessageFiles
import com.ismartcoding.plain.db.DMessageType
import com.ismartcoding.plain.helpers.AppHelper
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.helpers.TimeHelper
import androidx.core.net.toUri
import java.io.File

actual fun getFileId(path: String): String = FileHelper.getFileId(path)

actual fun deleteFileAt(path: String) {
    File(path).delete()
}

actual fun createLongTextFile(text: String): DMessageContent {
    val timestamp = TimeHelper.now().toEpochMilliseconds()
    val fileName = "message-$timestamp.txt"
    val dir = appContext.getExternalFilesDir(android.os.Environment.DIRECTORY_DOCUMENTS)
    if (!dir!!.exists()) dir.mkdirs()
    val file = java.io.File(dir, fileName)
    file.writeText(text)
    val summary = text.substring(0, minOf(text.length, Constants.TEXT_FILE_SUMMARY_LENGTH))
    val messageFile = DMessageFile(uri = file.absolutePath, size = file.length(), summary = summary, fileName = fileName)
    return DMessageContent(DMessageType.FILES.value, DMessageFiles(listOf(messageFile)))
}

actual fun saveFileToDownloads(path: String, fileName: String): String {
    return FileHelper.copyFileToDownloads(path, fileName)
}

actual fun fileToUriString(path: String): String = File(path).toUri().toString()

actual fun getFileIconPath(extension: String): String =
    AppHelper.getFileIconPath(extension)

actual fun fileExists(path: String): Boolean = File(path).exists()
