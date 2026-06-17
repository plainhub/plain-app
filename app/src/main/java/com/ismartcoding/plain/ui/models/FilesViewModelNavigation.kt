package com.ismartcoding.plain.ui.models

import android.content.Context
import com.ismartcoding.lib.extensions.appDir
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.enums.FilesType
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.features.file.ZipBrowserHelper
import com.ismartcoding.plain.preferences.LastFilePathPreference
import java.io.File

internal fun FilesViewModel.navigateToDirectoryInternal(context: Context, newPath: String) {
    if (selectedPath != newPath) {
        navigationHistoryInternal.add(selectedPath)
        selectedPath = newPath
        rebuildBreadcrumbsInternal(newPath)
        launchIO {
            isLoading.value = true
            updateItemsInternal(emptyList())
            loadAsync(context)
        }
    }
}

internal fun FilesViewModel.navigateBackInternal(): Boolean {
    return if (navigationHistoryInternal.isNotEmpty()) {
        selectedPath = navigationHistoryInternal.removeLastOrNull() ?: selectedPath
        rebuildBreadcrumbsInternal(selectedPath)
        true
    } else false
}

internal suspend fun FilesViewModel.loadLastPathAsyncInternal(context: Context) {
    val data = LastFilePathPreference.getValueAsync()
    withIO {
        if (data.selectedPath.isNotEmpty() && File(data.selectedPath).exists()) {
            type = inferFileTypeFromRootInternal(context, data.rootPath)
            initSelectedPathInternal(data.rootPath, type, data.fullPath, data.selectedPath)
        } else {
            type = inferFileTypeFromRootInternal(context, rootPath)
            updateRootBreadcrumb()
        }
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
    if (targetPath == rootPath) {
        selectedBreadcrumbIndex.value = 0
        return
    }
    if (ZipBrowserHelper.isZipPath(targetPath)) {
        // Build filesystem breadcrumbs up to the zip file
        val zipFilePath = ZipBrowserHelper.getZipFilePath(targetPath)
        val relativeToRoot = zipFilePath.removePrefix(rootPath).trimStart('/')
        if (relativeToRoot.isNotEmpty()) {
            var currentPath = rootPath
            relativeToRoot.split("/").forEach { segment ->
                if (segment.isNotEmpty()) {
                    currentPath += "/$segment"
                    // Zip file breadcrumb navigates to the zip root
                    val bcPath = if (currentPath == zipFilePath) {
                        ZipBrowserHelper.joinPath(zipFilePath, "")
                    } else {
                        currentPath
                    }
                    breadcrumbs.add(BreadcrumbItem(segment, bcPath))
                }
            }
        }
        // Build breadcrumbs for each internal directory component
        val internalPath = ZipBrowserHelper.getInternalPath(targetPath)
        val segments = internalPath.trimEnd('/').split("/").filter { it.isNotEmpty() }
        var currentInternalPath = ZipBrowserHelper.joinPath(zipFilePath, "")
        segments.forEach { segment ->
            val prevInternal = ZipBrowserHelper.getInternalPath(currentInternalPath)
            val newInternal = if (prevInternal.isEmpty()) "$segment/" else "$prevInternal$segment/"
            currentInternalPath = ZipBrowserHelper.joinPath(zipFilePath, newInternal)
            breadcrumbs.add(BreadcrumbItem(segment, currentInternalPath))
        }
    } else {
        val relativePath = targetPath.removePrefix(rootPath).trimStart('/')
        if (relativePath.isNotEmpty()) {
            var currentPath = rootPath
            relativePath.split("/").forEach { segment ->
                if (segment.isNotEmpty()) {
                    currentPath += "/$segment"
                    breadcrumbs.add(BreadcrumbItem(segment, currentPath))
                }
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

