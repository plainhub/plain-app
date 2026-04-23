package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.extensions.appDir
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.extensions.getParentPath
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.data.FilePathData
import com.ismartcoding.plain.enums.FilesType
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.preferences.LastFilePathPreference
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

internal fun FilesViewModel.navigateToDirectoryInternal(context: Context, newPath: String) {
    if (selectedPath != newPath) {
        navigationHistoryInternal.add(selectedPath)
        selectedPath = newPath
        getAndUpdateSelectedIndexInternal()
        viewModelScope.launch(Dispatchers.IO) {
            isLoading.value = true
            updateItemsInternal(emptyList())
            loadAsync(context)
        }
    }
}

internal fun FilesViewModel.navigateBackInternal(): Boolean {
    return if (navigationHistoryInternal.isNotEmpty()) {
        selectedPath = navigationHistoryInternal.removeLastOrNull() ?: selectedPath
        getAndUpdateSelectedIndexInternal()
        true
    } else false
}

internal suspend fun FilesViewModel.loadLastPathAsyncInternal(context: Context) {
    val data = LastFilePathPreference.getValueAsync(context)
    if (data.selectedPath.isNotEmpty() && File(data.selectedPath).exists()) {
        type = inferFileTypeFromRootInternal(context, data.rootPath)
        initSelectedPathInternal(data.rootPath, type, data.fullPath, data.selectedPath)
    } else {
        type = inferFileTypeFromRootInternal(context, rootPath)
        updateRootBreadcrumb()
    }
}

internal fun FilesViewModel.inferFileTypeFromRootInternal(context: Context, rootPath: String): FilesType {
    val internalStoragePath = FileSystemHelper.getInternalStoragePath()
    val appDataPath = context.appDir()
    val sdCardPath = FileSystemHelper.getSDCardPath(context)
    val usbPaths = FileSystemHelper.getUsbDiskPaths()
    return when {
        rootPath == appDataPath -> FilesType.APP
        rootPath == sdCardPath -> FilesType.SDCARD
        usbPaths.contains(rootPath) -> FilesType.USB_STORAGE
        rootPath == internalStoragePath -> FilesType.INTERNAL_STORAGE
        else -> FilesType.INTERNAL_STORAGE
    }
}

internal fun FilesViewModel.rebuildBreadcrumbsInternal(targetPath: String) {
    breadcrumbs.clear()
    breadcrumbs.add(BreadcrumbItem(getRootDisplayName(), rootPath))
    if (targetPath != rootPath) {
        val relativePath = targetPath.removePrefix(rootPath).trim('/')
        if (relativePath.isNotEmpty()) {
            var currentPath = rootPath
            relativePath.split("/").forEach { segment ->
                currentPath += "/$segment"
                breadcrumbs.add(BreadcrumbItem(segment, currentPath))
            }
        }
    }
    selectedBreadcrumbIndex.value = breadcrumbs.size - 1
}

internal fun FilesViewModel.initSelectedPathInternal(rootPath: String, type: FilesType, fullPath: String, selectedPath: String) {
    this.rootPath = rootPath
    this.type = type
    rebuildBreadcrumbsInternal(fullPath)
    this.selectedPath = selectedPath
    selectedBreadcrumbIndex.value = breadcrumbs.indexOfFirst { it.path == selectedPath }
    if (selectedBreadcrumbIndex.value == -1) selectedBreadcrumbIndex.value = breadcrumbs.size - 1
    navigationHistoryInternal.clear()
}

internal fun FilesViewModel.getAndUpdateSelectedIndexInternal(): Int {
    var index = breadcrumbs.indexOfFirst { it.path == selectedPath }
    if (index == -1) {
        val parent = selectedPath.getParentPath()
        breadcrumbs.reversed().forEach { b ->
            if (b.path != parent && !("$parent/").startsWith(b.path + "/")) breadcrumbs.remove(b)
        }
        breadcrumbs.add(BreadcrumbItem(selectedPath.getFilenameFromPath(), selectedPath))
        index = breadcrumbs.size - 1
    }
    selectedBreadcrumbIndex.value = index
    return index
}
