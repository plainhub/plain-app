package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.enums.FilesType
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.preferences.FavoriteFoldersPreference
import com.ismartcoding.plain.ui.models.FilesViewModel
import com.ismartcoding.plain.ui.models.FolderOption
import android.content.Context
import com.ismartcoding.lib.extensions.appDir
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

suspend fun buildFolderOptions(
    context: Context,
    filesVM: FilesViewModel,
    recentsText: String,
    internalStorageText: String,
    sdcardText: String,
    usbStorageText: String,
    fileTransferAssistantText: String,
): List<FolderOption> = withContext(Dispatchers.IO) {
    val internalStoragePath = FileSystemHelper.getInternalStoragePath()
    val externalFilesDirPath = context.appDir()
    val sdCardPath = FileSystemHelper.getSDCardPath(context)
    val usbPaths = FileSystemHelper.getUsbDiskPaths()
    val favoriteFolders = FavoriteFoldersPreference.getValueAsync(context)

    val allPaths = mutableListOf(internalStoragePath, externalFilesDirPath)
    if (sdCardPath.isNotEmpty()) allPaths.add(sdCardPath)
    allPaths.addAll(usbPaths)
    favoriteFolders.forEach { fav ->
        if (java.io.File(fav.fullPath).exists()) allPaths.add(fav.fullPath)
    }

    val longestMatchPath = allPaths.filter { filesVM.selectedPath.startsWith(it) }.maxByOrNull { it.length } ?: ""

    val menuItems = mutableListOf(
        FolderOption(rootPath = "", fullPath = "", type = FilesType.RECENTS, isChecked = filesVM.type == FilesType.RECENTS, title = recentsText),
        FolderOption(rootPath = internalStoragePath, fullPath = internalStoragePath, type = FilesType.INTERNAL_STORAGE, isChecked = longestMatchPath == internalStoragePath, title = internalStorageText),
    )

    if (sdCardPath.isNotEmpty()) {
        menuItems.add(FolderOption(rootPath = sdCardPath, fullPath = sdCardPath, type = FilesType.SDCARD, isChecked = longestMatchPath == sdCardPath, title = sdcardText))
    }
    usbPaths.forEachIndexed { index, path ->
        menuItems.add(FolderOption(rootPath = path, fullPath = path, type = FilesType.USB_STORAGE, isChecked = longestMatchPath == path, title = "$usbStorageText ${index + 1}"))
    }
    menuItems.add(FolderOption(rootPath = externalFilesDirPath, fullPath = externalFilesDirPath, type = FilesType.APP, isChecked = longestMatchPath == externalFilesDirPath, title = fileTransferAssistantText))

    favoriteFolders.forEach { fav ->
        if (java.io.File(fav.fullPath).exists()) {
            val rootName = when {
                fav.rootPath == internalStoragePath -> FileSystemHelper.getInternalStorageName()
                fav.rootPath == externalFilesDirPath -> fileTransferAssistantText
                fav.rootPath == sdCardPath -> sdcardText
                usbPaths.contains(fav.rootPath) -> "$usbStorageText ${usbPaths.indexOf(fav.rootPath) + 1}"
                else -> fav.rootPath
            }
            val relativePath = if (fav.fullPath.startsWith(fav.rootPath)) fav.fullPath.removePrefix(fav.rootPath).removePrefix("/") else java.io.File(fav.fullPath).name
            val displayTitle = if (relativePath.isNotEmpty()) "$rootName/$relativePath" else rootName
            menuItems.add(FolderOption(rootPath = fav.rootPath, fullPath = fav.fullPath, type = FilesType.INTERNAL_STORAGE, isChecked = longestMatchPath == fav.fullPath, title = displayTitle, isFavoriteFolder = true))
        }
    }
    menuItems
}
