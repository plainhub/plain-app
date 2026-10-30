package com.ismartcoding.plain.ui.files

import com.ismartcoding.plain.rust.graphql.ContentSource


import com.ismartcoding.plain.features.files.BreadcrumbItem

import com.ismartcoding.plain.ui.state.ISearchableViewModel
import com.ismartcoding.plain.ui.state.ISelectableViewModel
import com.ismartcoding.plain.ui.state.launchSafe

import com.ismartcoding.plain.models.File as FileEntry

import com.ismartcoding.plain.Prefs

import com.ismartcoding.plain.i18n.*

import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ismartcoding.plain.features.files.FilePathData
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.features.files.FilesType
import com.ismartcoding.plain.platform.system.hasPermission
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.features.files.ZipBrowserHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.i18n.LocaleHelper
import com.ismartcoding.plain.platform.system.appDir
import com.ismartcoding.plain.platform.files.fileExists
import com.ismartcoding.plain.platform.files.getInternalStorageName
import com.ismartcoding.plain.platform.files.getInternalStoragePath
import com.ismartcoding.plain.platform.files.getSDCardPath
import com.ismartcoding.plain.platform.files.getUsbDiskPaths
import com.ismartcoding.plain.ui.components.DialogHelper
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException

class FilesViewModel internal constructor(val source: ContentSource = ContentSource.Local) : ISearchableViewModel<FileEntry>, ISelectableViewModel<FileEntry>, ViewModel() {
    var rootPath = source.rootPath
    private var _selectedPath = rootPath
    var selectedPath: String
        get() = _selectedPath
        set(value) {
            val isChanged = _selectedPath != value
            _selectedPath = value
            if (isChanged) {
                selectedPathVersion.value++
                val fullPath = breadcrumbs.lastOrNull()?.path ?: value
                val savedPath = FilePathData(rootPath = rootPath, fullPath = fullPath, selectedPath = value)
                viewModelScope.launchSafe { source.locations.saveLastPath(savedPath) }
            }
        }

    /** Bumped whenever the selected path changes, so observers (e.g. the drawer folder list) can refresh. */
    val selectedPathVersion = mutableIntStateOf(0)

    val breadcrumbs = mutableStateListOf<BreadcrumbItem>()
    val selectedBreadcrumbIndex = mutableIntStateOf(0)
    var cutFiles = mutableListOf<FileEntry>()
    var copyFiles = mutableListOf<FileEntry>()
    var type: FilesType = FilesType.INTERNAL_STORAGE
    val hasPermission = mutableStateOf(when (source) {
        ContentSource.Local -> AppFeatureType.FILES.hasPermission()
        is ContentSource.Remote -> com.ismartcoding.plain.platform.permissions.Permission.WRITE_EXTERNAL_STORAGE in source.info.permissions
    })
    var offset = 0
    var limit: Int = 1000
    var total: Int = 0
    internal val navigationHistoryInternal = mutableStateListOf<String>()

    init { breadcrumbs.add(BreadcrumbItem(getRootDisplayName(), rootPath)) }

    val selectedFile = mutableStateOf<FileEntry?>(null)
    override val showSearchBar = mutableStateOf(false)
    override val searchActive = mutableStateOf(false)
    override val queryText = mutableStateOf("")
    override val selectMode = mutableStateOf(false)
    override val selectedIds = mutableStateListOf<String>()
    private val _itemsFlow = MutableStateFlow<List<FileEntry>>(emptyList())
    override val itemsFlow: StateFlow<List<FileEntry>> = _itemsFlow.asStateFlow()
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

    internal fun updateItemsInternal(items: List<FileEntry>) { _itemsFlow.value = items }

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
        val version = selectedPathVersion.intValue
        val data = source.locations.lastPath()
        val exists = data.selectedPath.isNotEmpty() && source.exists(data.selectedPath)
        if (version != selectedPathVersion.intValue) return@withIO
        if (exists) {
            type = inferFileTypeFromRoot(data.rootPath)
            initSelectedPath(data.rootPath, type, data.selectedPath, data.selectedPath)
        } else {
            type = inferFileTypeFromRoot(rootPath)
            updateRootBreadcrumb()
        }
    }

    fun inferFileTypeFromRoot(rootPath: String): FilesType {
        if (source is ContentSource.Remote) return if (rootPath == source.appPath) FilesType.APP else FilesType.INTERNAL_STORAGE
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
        val showHiddenFiles = Prefs.showHiddenFiles.value
        withIO {
            isLoading.value = true
            try {
                val version = selectedPathVersion.intValue
                val path = selectedPath
                val query = getQuery()
                val files = when {
                    ZipBrowserHelper.isZipPath(path) -> source.files.list(path, true, sortBy.value)
                    showSearchBar.value && query.isNotEmpty() -> source.files.searchName(query, path, showHiddenFiles, sortBy.value)
                    type == FilesType.RECENTS -> source.files.recent()
                    else -> source.files.list(path, showHiddenFiles, sortBy.value)
                }
                if (version == selectedPathVersion.intValue) _itemsFlow.value = files
            } finally { isLoading.value = false }
        }
    }

    fun deleteFiles(paths: Set<String>) {
        viewModelScope.launch {
            DialogHelper.showLoading()
            try {
                withIO {
                    source.files.deleteMany(paths.toList())
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (error: Exception) { DialogHelper.showMessage(error) }
            finally {
                DialogHelper.hideLoading()
                loadAsync()
            }
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
