package com.ismartcoding.plain.ui.models

import android.content.Context
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.toMutableStateList
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.extensions.getParentPath
import com.ismartcoding.lib.extensions.scanFileByConnection
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.FilesType
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.media.FileMediaStoreHelper
import com.ismartcoding.plain.preference.FilePathData
import com.ismartcoding.plain.preference.LastFilePathPreference
import com.ismartcoding.plain.preference.ShowHiddenFilesPreference
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.util.Stack

data class BreadcrumbItem(var name: String, var path: String)

class FilesViewModel : ISearchableViewModel<DFile>, ISelectableViewModel<DFile>, ViewModel() {
    var root = FileSystemHelper.getInternalStoragePath()
    private var _path = root
    var path: String
        get() = _path
        set(value) {
            val isChanged = _path != value
            _path = value
            if (isChanged) {
                viewModelScope.launch(Dispatchers.IO) {
                    // Save complete breadcrumbs path to preserve user's navigation state
                    val breadcrumbsCopy = breadcrumbs.toList()
                    val fullPath = if (breadcrumbsCopy.isNotEmpty()) {
                        breadcrumbsCopy.last().path
                    } else {
                        value
                    }
                    LastFilePathPreference.putAsync(
                        MainApp.instance,
                        FilePathData(
                            rootPath = root, 
                            fullPath = fullPath, 
                            selectedPath = value
                        )
                    )
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

    private val navigationHistory = Stack<String>()

    init {
        // Initialize with default breadcrumb - will be updated when loadLastPathAsync is called
        breadcrumbs.add(BreadcrumbItem(getRootDisplayName(), root))
    }

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

    private fun updateItems(items: List<DFile>) {
        _itemsFlow.value = items
    }

    fun navigateToDirectory(context: Context, newPath: String) {
        if (path != newPath) {
            // Add current path to history before changing
            navigationHistory.push(path)
            path = newPath
            getAndUpdateSelectedIndex()
            viewModelScope.launch(Dispatchers.IO) {
                isLoading.value = true
                updateItems(emptyList())
                loadAsync(context)
            }
        }
    }

    fun navigateBack(): Boolean {
        return if (navigationHistory.isNotEmpty()) {
            path = navigationHistory.pop()
            getAndUpdateSelectedIndex()
            true
        } else {
            false
        }
    }

    suspend fun loadLastPathAsync(context: Context) {
        val data = LastFilePathPreference.getValueAsync(context)
        if (data.selectedPath.isNotEmpty() && File(data.selectedPath).exists()) {
            root = data.rootPath
            // Infer the file system type from the root path
            type = inferFileTypeFromRoot(context, data.rootPath)
            // Restore complete breadcrumbs from fullPath
            rebuildBreadcrumbs(data.fullPath)
            _path = data.selectedPath
            // Set correct breadcrumb selection
            selectedBreadcrumbIndex.value = breadcrumbs.indexOfFirst { it.path == data.selectedPath }
            if (selectedBreadcrumbIndex.value == -1) {
                selectedBreadcrumbIndex.value = breadcrumbs.size - 1
            }
            navigationHistory.clear()
        } else {
            // No saved path, but still infer the type based on current root
            type = inferFileTypeFromRoot(context, root)
            updateRootBreadcrumb()
        }
    }

    private fun inferFileTypeFromRoot(context: Context, rootPath: String): FilesType {
        val internalStoragePath = FileSystemHelper.getInternalStoragePath()
        val appDataPath = FileSystemHelper.getExternalFilesDirPath(context)
        val sdCardPath = FileSystemHelper.getSDCardPath(context)
        val usbPaths = FileSystemHelper.getUsbDiskPaths()

        return when {
            rootPath == appDataPath -> FilesType.APP
            rootPath == sdCardPath -> FilesType.SDCARD
            usbPaths.contains(rootPath) -> FilesType.USB_STORAGE
            rootPath == internalStoragePath -> FilesType.INTERNAL_STORAGE
            else -> FilesType.INTERNAL_STORAGE // default fallback
        }
    }

    private fun rebuildBreadcrumbs(targetPath: String) {
        breadcrumbs.clear()
        breadcrumbs.add(BreadcrumbItem(getRootDisplayName(), root))
        
        if (targetPath != root) {
            val relativePath = targetPath.removePrefix(root).trim('/')
            if (relativePath.isNotEmpty()) {
                var currentPath = root
                relativePath.split("/").forEach { segment ->
                    currentPath += "/$segment"
                    breadcrumbs.add(BreadcrumbItem(segment, currentPath))
                }
            }
        }
        
        selectedBreadcrumbIndex.value = breadcrumbs.size - 1
    }

    fun getRootDisplayName(): String {
        return when (type) {
            FilesType.INTERNAL_STORAGE -> FileSystemHelper.getInternalStorageName()
            FilesType.APP -> LocaleHelper.getString(R.string.app_data)
            FilesType.SDCARD -> LocaleHelper.getString(R.string.sdcard)
            FilesType.USB_STORAGE -> LocaleHelper.getString(R.string.usb_storage)
            FilesType.RECENTS -> LocaleHelper.getString(R.string.recents)
        }
    }

    fun updateRootBreadcrumb() {
        if (breadcrumbs.isNotEmpty()) {
            breadcrumbs[0] = BreadcrumbItem(getRootDisplayName(), root)
        }
    }

    // Check if we can navigate back
    fun canNavigateBack(): Boolean {
        return navigationHistory.isNotEmpty()
    }

    // Initialize path without adding to history
    fun initPath(newPath: String) {
        path = newPath
        // Clear navigation history when initializing
        navigationHistory.clear()
        getAndUpdateSelectedIndex()
    }

    private fun getAndUpdateSelectedIndex(): Int {
        var index = breadcrumbs.indexOfFirst { it.path == path }
        if (index == -1) {
            val parent = path.getParentPath()
            breadcrumbs.reversed().forEach { b ->
                if (b.path != parent && !("$parent/").startsWith(b.path + "/")) {
                    breadcrumbs.remove(b)
                }
            }
            breadcrumbs.add(BreadcrumbItem(path.getFilenameFromPath(), path))
            index = breadcrumbs.size - 1
        }

        selectedBreadcrumbIndex.value = index
        return index
    }

    fun getQuery(): String {
        return queryText.value.trim()
    }

    suspend fun loadAsync(context: android.content.Context) {
        isLoading.value = true
        val showHiddenFiles = ShowHiddenFilesPreference.getAsync(context)
        val query = getQuery()
        val files = if (showSearchBar.value && query.isNotEmpty()) {
            FileSystemHelper.search(query, path, showHiddenFiles)
        } else if (type == FilesType.RECENTS) {
            FileMediaStoreHelper.getRecentFilesAsync(context)
        } else {
            FileSystemHelper.getFilesList(
                path,
                showHiddenFiles,
                sortBy.value
            )
        }

        _itemsFlow.value = files
        isLoading.value = false
    }

    fun deleteFiles(paths: Set<String>) {
        viewModelScope.launch {
            DialogHelper.showLoading()
            withIO {
                paths.forEach {
                    File(it).deleteRecursively()
                }

                MainApp.instance.scanFileByConnection(paths.toTypedArray())
            }
            DialogHelper.hideLoading()

            _itemsFlow.update {
                it.toMutableStateList().apply {
                    removeIf { i -> paths.contains(i.path) }
                }
            }
        }
    }
}

