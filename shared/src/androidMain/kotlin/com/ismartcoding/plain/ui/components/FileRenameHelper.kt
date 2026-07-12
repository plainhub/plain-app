package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.helpers.FileHelper
import com.ismartcoding.plain.lib.extensions.scanFileByConnection

internal suspend fun renameAndScan(path: String, newName: String): String? {
    val newFile = FileHelper.rename(path, newName)
    appContext.scanFileByConnection(path)
    if (newFile != null) {
        appContext.scanFileByConnection(newFile.absolutePath)
    }
    return newFile?.absolutePath
}
