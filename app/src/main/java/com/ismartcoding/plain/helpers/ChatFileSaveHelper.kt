package com.ismartcoding.plain.helpers

import android.content.Context
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.preferences.ChatFilesSaveFolderPreference
import java.io.File

data class ChatFilePath(val path: String, val dir: String, val isAppDirectory: Boolean = false) {
    fun getFinalPath(): String {
        return if (isAppDirectory) {
            "app://${path.getFilenameFromPath()}"
        } else {
            path
        }
    }
}

object ChatFileSaveHelper {
    suspend fun generateChatFilePathAsync(
        context: Context,
        fileName: String,
        pickFileType: PickFileType? = null
    ): ChatFilePath {
        var actualFileName = fileName

        if (pickFileType == PickFileType.IMAGE_VIDEO && !actualFileName.contains(".")) {
            actualFileName = "$actualFileName.jpg"
        }

        val customDir = ChatFilesSaveFolderPreference.getAsync(context)
        val targetDir =  if (customDir.isEmpty()) {
            context.getExternalFilesDir(null)!!
        } else {
            val dir = File(customDir)
            if (!dir.exists()) {
                dir.mkdirs()
            }
            dir
        }

        var destFile = File(targetDir.absolutePath, actualFileName)
        if (destFile.exists()) {
            destFile = generateUniqueFile(destFile)
        }

        val prefix = FileSystemHelper.getExternalFilesDirPath(context)
        val isAppDirectory = targetDir.absolutePath.startsWith(prefix)

        return ChatFilePath(destFile.absolutePath, targetDir.absolutePath, isAppDirectory)
    }

    private fun generateUniqueFile(originalFile: File): File {
        val nameWithoutExt = originalFile.nameWithoutExtension
        val ext = originalFile.extension
        var index = 1
        var newFile: File

        do {
            val newName = if (ext.isNotEmpty()) {
                "$nameWithoutExt($index).$ext"
            } else {
                "$nameWithoutExt($index)"
            }
            newFile = File(originalFile.parent, newName)
            index++
        } while (newFile.exists())

        return newFile
    }
} 