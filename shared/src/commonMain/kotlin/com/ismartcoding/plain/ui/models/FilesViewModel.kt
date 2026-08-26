package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.i18n.*

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.data.FilePathData
import com.ismartcoding.plain.enums.FilesType
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.file.ZipBrowserHelper
import com.ismartcoding.plain.helpers.FilePathValidator
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.appDir
import com.ismartcoding.plain.platform.deleteFileOrDir
import com.ismartcoding.plain.platform.fileExists
import com.ismartcoding.plain.platform.getInternalStorageName
import com.ismartcoding.plain.platform.getInternalStoragePath
import com.ismartcoding.plain.platform.getRecentFiles
import com.ismartcoding.plain.platform.getSDCardPath
import com.ismartcoding.plain.platform.getUsbDiskPaths
import com.ismartcoding.plain.platform.listFilesInDir
import com.ismartcoding.plain.platform.listZipEntries
import com.ismartcoding.plain.platform.scanFiles
import com.ismartcoding.plain.platform.searchFilesByName
import com.ismartcoding.plain.preferences.LastFilePathPreference
import com.ismartcoding.plain.preferences.ShowHiddenFilesPreference
import com.ismartcoding.plain.ui.helpers.DialogHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class FilesViewModel : ISearchableViewModel<DFile>, ISelectableViewModel<DFile>, ViewModel() {
    var rootPath = getInternalStoragePath()
    private var _selectedPath = rootPath
    var selectedPath: String
        get() = _selectedPath
        set(value) {
            val isChanged = _selectedPath != value
            _selectedPath = value
            if (isChanged) {
                selectedPathVersion.value++
                viewModelScope.launchSafe {
                    val breadcrumbsCopy = breadcrumbs.toList()
                    val fullPath = if (breadcrumbsCopy.isNotEmpty()) breadcrumbsCopy.last().path else value
                    LastFilePathPreference.putAsync(FilePathData(rootPath = rootPath, fullPath = fullPath, selectedPath = value))
                }
            }
        }

    /** Bumped whenever the selected path changes, so observers (e.g. the drawer folder list) can refresh. */
    val selectedPathVersion = mutableIntStateOf(0)

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
    val showCreateShareDialog = mutableStateOf(false)
    val sharePaths = mutableStateListOf<String>()
    val isDeleting = mutableStateOf(false)
    val favoriteFoldersVersion = mutableIntStateOf(0)

    /** Bumped whenever the share list changes, so the drawer share section can refresh. */
    val sharesVersion = mutableIntStateOf(0)

    /**
     * Title override for the folder picked from the drawer, stored as
     * (fullPath to title) — e.g. a favorite folder's alias. Applies only
     * while the selected path matches, so navigating away reverts the title.
     */
    val drawerFolderTitle = mutableStateOf<Pair<String, String>?>(null)

    fun currentFolderTitleOverride(): String? = drawerFolderTitle.value?.takeIf { it.first == selectedPath }?.second

    internal fun updateItemsInternal(items: List<DFile>) { _itemsFlow.value = items }

    fun navigateToDirectory(newPath: String) {
        if (selectedPath != newPath) {
            navigationHistoryInternal.add(selectedPath)
            rebuildBreadcrumbs(newPath)
            selectedPath = newPath
            viewModelScope.launchSafe {
                isLoading.value = true
                updateItemsInternal(emptyList())
                loadAsync()
            }
        }
    }

    fun navigateBack(): Boolean {
        return if (navigationHistoryInternal.isNotEmpty()) {
            val prevPath = navigationHistoryInternal.removeLastOrNull() ?: selectedPath
            rebuildBreadcrumbs(prevPath)
            selectedPath = prevPath
            true
        } else false
    }

    suspend fun loadLastPathAsync() = withIO {
        val data = LastFilePathPreference.getValueAsync()
        if (data.selectedPath.isNotEmpty() && fileExists(data.selectedPath)) {
            type = inferFileTypeFromRoot(data.rootPath)
            initSelectedPath(data.rootPath, type, data.selectedPath, data.selectedPath)
        } else {
            type = inferFileTypeFromRoot(rootPath)
            updateRootBreadcrumb()
        }
    }

    fun inferFileTypeFromRoot(rootPath: String): FilesType {
        val internalStoragePath = getInternalStoragePath()
        val appDataPath = appDir()
        val sdCardPath = getSDCardPath()
        val usbPaths = getUsbDiskPaths()
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
            val (items, _) = buildRegularBreadcrumbs(rootPath, getRootDisplayName(), targetPath)
            breadcrumbs.clear()
            breadcrumbs.addAll(items)
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
        FilesType.INTERNAL_STORAGE -> getInternalStorageName()
        FilesType.APP -> LocaleHelper.getString(Res.string.app_data)
        FilesType.SDCARD -> LocaleHelper.getString(Res.string.sdcard)
        FilesType.USB_STORAGE -> LocaleHelper.getString(Res.string.usb_storage)
        FilesType.RECENTS -> LocaleHelper.getString(Res.string.recents)
    }

    fun updateRootBreadcrumb() { if (breadcrumbs.isNotEmpty()) breadcrumbs[0] = BreadcrumbItem(getRootDisplayName(), rootPath) }
    fun getQuery(): String = queryText.value.trim()

    suspend fun loadAsync() {
        val showHiddenFiles = ShowHiddenFilesPreference.getAsync()
        withIO {
            isLoading.value = true
            val query = getQuery()
            val files = when {
                ZipBrowserHelper.isZipPath(selectedPath) -> listZipEntries(selectedPath, sortBy.value)
                showSearchBar.value && query.isNotEmpty() -> searchFilesByName(query, selectedPath, showHiddenFiles, sortBy.value)
                type == FilesType.RECENTS -> getRecentFiles()
                else -> listFilesInDir(selectedPath, showHiddenFiles, sortBy.value)
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
                paths.forEach { deleteFileOrDir(it) }
                scanFiles(paths.toTypedArray())
            }
            DialogHelper.hideLoading()
            _itemsFlow.update { it.filterNot { i -> paths.contains(i.path) } }
        }
    }

    internal companion object {
        /**
         * Builds breadcrumb items for a regular (non-zip) directory [targetPath] under [rootPath].
         * Returns the breadcrumb list (root first) and the index of the deepest segment.
         */
        fun buildRegularBreadcrumbs(rootPath: String, rootName: String, targetPath: String): Pair<List<BreadcrumbItem>, Int> {
            val items = mutableListOf(BreadcrumbItem(rootName, rootPath))
            if (targetPath == rootPath) return items to 0
            val relativePath = targetPath.removePrefix(rootPath).trimStart('/')
            if (relativePath.isNotEmpty()) {
                var currentPath = rootPath
                relativePath.split("/").forEach { segment ->
                    if (segment.isNotEmpty()) {
                        currentPath += "/$segment"
                        items.add(BreadcrumbItem(segment, currentPath))
                    }
                }
            }
            return items to (items.size - 1)
        }
    }
}
