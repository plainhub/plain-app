package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.i18n.*

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.lib.extensions.appDir
import com.ismartcoding.plain.lib.extensions.scanFileByConnection
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.appContext
import com.ismartcoding.plain.data.FilePathData
import com.ismartcoding.plain.enums.FilesType
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.features.file.ZipBrowserHelper
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.media.FileMediaStoreHelper
import com.ismartcoding.plain.preferences.LastFilePathPreference
import com.ismartcoding.plain.preferences.ShowHiddenFilesPreference
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.helpers.FilePathValidator
import com.ismartcoding.plain.helpers.launchSafe
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File

data class BreadcrumbItem(var name: String, var path: String)

class FilesViewModel : ISearchableViewModel<DFile>, ISelectableViewModel<DFile>, ViewModel() {
    var rootPath = FileSystemHelper.getInternalStoragePath()
    private var _selectedPath = rootPath
    var selectedPath: String
        get() = _selectedPath
        set(value) {
            val isChanged = _selectedPath != value
            _selectedPath = value
            if (isChanged) {
                launchSafe {
                    val breadcrumbsCopy = breadcrumbs.toList()
                    val fullPath = if (breadcrumbsCopy.isNotEmpty()) breadcrumbsCopy.last().path else value
                    LastFilePathPreference.putAsync(FilePathData(rootPath = rootPath, fullPath = fullPath, selectedPath = value))
                }
            }
        }

    val breadcrumbs = mutableStateListOf<BreadcrumbItem>()
    val selectedBreadcrumbIndex = mutableIntStateOf(0)
    var cutFiles = mutableListOf<DFile>()
    var copyFiles = mutableListOf<DFile>()
    var type: FilesType = FilesType.INTERNAL_STORAGE
    var offset = 0
    var limit: Int = 1000
    var total: Int = 0
    internal val navigationHistoryInternal = mutableStateListOf<String>()

    init { breadcrumbs.add(BreadcrumbItem(getRootDisplayName(), rootPath)) }

    val selectedFile = mutableStateOf<DFile?>(null)
    val showRenameDialog = mutableStateOf(false)
    override val showSearchBar = mutableStateOf(false)
    override val searchActive = mutableStateOf(false)
    override val queryText = mutableStateOf("")
    override val selectMode = mutableStateOf(false)
    override val selectedIds = mutableStateListOf<String>()
    private val _itemsFlow = MutableStateFlow<List<DFile>>(emptyList())
    override val itemsFlow: StateFlow<List<DFile>> = _itemsFlow.asStateFlow()
    val sortBy = mutableStateOf(FileSortBy.NAME_ASC)
    val showSortDialog = mutableStateOf(false)
    val isLoading = mutableStateOf(true)
    val showPasteBar = mutableStateOf(false)
    val showCreateFolderDialog = mutableStateOf(false)
    val showCreateFileDialog = mutableStateOf(false)
    val showFolderKanbanDialog = mutableStateOf(false)
    val isDeleting = mutableStateOf(false)

    internal fun updateItemsInternal(items: List<DFile>) { _itemsFlow.value = items }

    fun navigateToDirectory(context: Context, newPath: String) {
        if (selectedPath != newPath) {
            navigationHistoryInternal.add(selectedPath)
            selectedPath = newPath
            rebuildBreadcrumbs(newPath)
            launchSafe {
                isLoading.value = true
                updateItemsInternal(emptyList())
                loadAsync(context)
            }
        }
    }

    fun navigateBack(): Boolean {
        return if (navigationHistoryInternal.isNotEmpty()) {
            selectedPath = navigationHistoryInternal.removeLastOrNull() ?: selectedPath
            rebuildBreadcrumbs(selectedPath)
            true
        } else false
    }

    suspend fun loadLastPathAsync(context: Context) = withIO {
        val data = LastFilePathPreference.getValueAsync()
        if (data.selectedPath.isNotEmpty() && File(data.selectedPath).exists()) {
            type = inferFileTypeFromRoot(context, data.rootPath)
            initSelectedPath(data.rootPath, type, data.fullPath, data.selectedPath)
        } else {
            type = inferFileTypeFromRoot(context, rootPath)
            updateRootBreadcrumb()
        }
    }

    fun inferFileTypeFromRoot(context: Context, rootPath: String): FilesType {
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

    fun rebuildBreadcrumbs(targetPath: String) {
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

    fun initSelectedPath(rootPath: String, type: FilesType, fullPath: String, selectedPath: String) {
        this.rootPath = rootPath
        this.type = type
        rebuildBreadcrumbs(fullPath)
        this.selectedPath = selectedPath
        selectedBreadcrumbIndex.value = breadcrumbs.indexOfFirst { it.path == selectedPath }
        if (selectedBreadcrumbIndex.value == -1) selectedBreadcrumbIndex.value = breadcrumbs.size - 1
        navigationHistoryInternal.clear()
    }

    fun canNavigateBack(): Boolean = navigationHistoryInternal.isNotEmpty()

    fun getRootDisplayName(): String = when (type) {
        FilesType.INTERNAL_STORAGE -> FileSystemHelper.getInternalStorageName()
        FilesType.APP -> LocaleHelper.getString(Res.string.app_data)
        FilesType.SDCARD -> LocaleHelper.getString(Res.string.sdcard)
        FilesType.USB_STORAGE -> LocaleHelper.getString(Res.string.usb_storage)
        FilesType.RECENTS -> LocaleHelper.getString(Res.string.recents)
    }

    fun updateRootBreadcrumb() { if (breadcrumbs.isNotEmpty()) breadcrumbs[0] = BreadcrumbItem(getRootDisplayName(), rootPath) }
    fun getQuery(): String = queryText.value.trim()

    suspend fun loadAsync(context: Context) {
        val showHiddenFiles = ShowHiddenFilesPreference.getAsync()
        withIO {
            isLoading.value = true
            val query = getQuery()
            val files = when {
                ZipBrowserHelper.isZipPath(selectedPath) -> ZipBrowserHelper.listEntries(selectedPath, sortBy.value)
                showSearchBar.value && query.isNotEmpty() -> FileSystemHelper.search(query, selectedPath, showHiddenFiles)
                type == FilesType.RECENTS -> FileMediaStoreHelper.getRecentFilesAsync(context)
                else -> FileSystemHelper.getFilesList(selectedPath, showHiddenFiles, sortBy.value)
            }
            _itemsFlow.value = files
            isLoading.value = false
        }
    }

    fun deleteFiles(paths: Set<String>) {
        viewModelScope.launch {
            DialogHelper.showLoading()
            withIO {
                FilePathValidator.requireAllSafe(paths.toList())
                paths.forEach { File(it).deleteRecursively() }
                appContext.scanFileByConnection(paths.toTypedArray())
            }
            DialogHelper.hideLoading()
            _itemsFlow.update { it.filterNot { i -> paths.contains(i.path) } }
        }
    }
}
