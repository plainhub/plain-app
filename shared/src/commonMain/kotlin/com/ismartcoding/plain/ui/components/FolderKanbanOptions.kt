package com.ismartcoding.plain.ui.components

import com.ismartcoding.plain.features.files.FavoriteFoldersClient
import com.ismartcoding.plain.features.files.FilesType
import com.ismartcoding.plain.lib.extensions.getFilenameFromPath
import com.ismartcoding.plain.platform.system.appDir
import com.ismartcoding.plain.platform.files.fileExists
import com.ismartcoding.plain.platform.files.getInternalStorageName
import com.ismartcoding.plain.platform.files.getInternalStoragePath
import com.ismartcoding.plain.platform.files.getSDCardPath
import com.ismartcoding.plain.platform.files.getUsbDiskPaths
import com.ismartcoding.plain.ui.files.FilesViewModel
import com.ismartcoding.plain.features.files.FolderOption
import com.ismartcoding.plain.lib.withIO

suspend fun buildFolderOptions(
    filesVM: FilesViewModel,
    recentsText: String,
    internalStorageText: String,
    sdcardText: String,
    usbStorageText: String,
    fileTransferAssistantText: String,
): List<FolderOption> = withIO {
    val source = filesVM.source
    if (source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) {
        val roots = source.client.roots()
        val favorites = source.favorites.refresh()
        val paths = roots.map { it.path } + source.appPath + favorites.map { it.fullPath }
        val selected = paths.filter { filesVM.selectedPath.startsWith(it) }.maxByOrNull { it.length }
        return@withIO buildList {
            add(FolderOption(rootPath = "", fullPath = "", type = FilesType.RECENTS, isChecked = filesVM.type == FilesType.RECENTS, title = recentsText))
            roots.filter { it.path != source.appPath }.forEach { root ->
                add(FolderOption(rootPath = root.path, fullPath = root.path, type = FilesType.INTERNAL_STORAGE, isChecked = selected == root.path, title = root.name))
            }
            add(FolderOption(rootPath = source.appPath, fullPath = source.appPath, type = FilesType.APP, isChecked = selected == source.appPath, title = fileTransferAssistantText))
            favorites.forEach { favorite ->
                add(FolderOption(rootPath = favorite.rootPath, fullPath = favorite.fullPath, type = filesVM.inferFileTypeFromRoot(favorite.rootPath), isChecked = selected == favorite.fullPath, title = favorite.alias ?: favorite.fullPath, isFavoriteFolder = true))
            }
        }
    }
    val internalStoragePath = getInternalStoragePath()
    val externalFilesDirPath = appDir()
    val sdCardPath = getSDCardPath()
    val usbPaths = getUsbDiskPaths()
    val favoriteFolders = FavoriteFoldersClient.refresh()

    val allPaths = mutableListOf(internalStoragePath, externalFilesDirPath)
    if (sdCardPath.isNotEmpty()) allPaths.add(sdCardPath)
    allPaths.addAll(usbPaths)
    favoriteFolders.forEach { fav ->
        if (fileExists(fav.fullPath)) allPaths.add(fav.fullPath)
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
        if (fileExists(fav.fullPath)) {
            val rootName = when {
                fav.rootPath == internalStoragePath -> getInternalStorageName()
                fav.rootPath == externalFilesDirPath -> fileTransferAssistantText
                fav.rootPath == sdCardPath -> sdcardText
                usbPaths.contains(fav.rootPath) -> "$usbStorageText ${usbPaths.indexOf(fav.rootPath) + 1}"
                else -> fav.rootPath
            }
            val relativePath = if (fav.fullPath.startsWith(fav.rootPath)) fav.fullPath.removePrefix(fav.rootPath).removePrefix("/") else fav.fullPath.getFilenameFromPath()
            val displayTitle = if (relativePath.isNotEmpty()) "$rootName/$relativePath" else rootName
            menuItems.add(FolderOption(rootPath = fav.rootPath, fullPath = fav.fullPath, type = FilesType.INTERNAL_STORAGE, isChecked = longestMatchPath == fav.fullPath, title = fav.alias ?: displayTitle, isFavoriteFolder = true))
        }
    }
    menuItems
}
