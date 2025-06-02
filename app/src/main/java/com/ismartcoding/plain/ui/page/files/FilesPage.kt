package com.ismartcoding.plain.ui.page.files

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.extensions.getFilenameFromPath
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.ActionSourceType
import com.ismartcoding.plain.enums.FilesType
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.PermissionsResultEvent
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.preference.FileSortByPreference
import com.ismartcoding.plain.preference.ShowHiddenFilesPreference
import com.ismartcoding.plain.ui.base.ActionButtonFolderKanban
import com.ismartcoding.plain.ui.base.ActionButtonMoreWithMenu
import com.ismartcoding.plain.ui.base.ActionButtonSearch
import com.ismartcoding.plain.ui.base.ActionButtonSort
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.base.PDropdownMenuItemCreateFile
import com.ismartcoding.plain.ui.base.PDropdownMenuItemCreateFolder
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopRightButton
import com.ismartcoding.plain.ui.base.SearchableTopBar
import com.ismartcoding.plain.ui.base.TextFieldDialog
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.FileSortDialog
import com.ismartcoding.plain.ui.components.FolderKanbanDialog
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewer
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.BreadcrumbItem
import com.ismartcoding.plain.ui.models.DrawerMenuItemClickedEvent
import com.ismartcoding.plain.ui.models.FilesViewModel
import com.ismartcoding.plain.ui.models.enterSearchMode
import com.ismartcoding.plain.ui.models.exitSearchMode
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.isAllSelected
import com.ismartcoding.plain.ui.models.showBottomActions
import com.ismartcoding.plain.ui.models.toggleSelectAll
import com.ismartcoding.plain.ui.page.files.components.BreadcrumbView
import com.ismartcoding.plain.ui.page.files.components.FileListContent
import com.ismartcoding.plain.ui.page.files.components.FilePasteBar
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FilesPage(
    navController: NavHostController,
    fileType: FilesType? = null,
    audioPlaylistVM: AudioPlaylistViewModel,
    filesVM: FilesViewModel = viewModel(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()
    val previewerState = rememberPreviewerState()
    val sharedFlow = Channel.sharedFlow

    val itemsState by filesVM.itemsFlow.collectAsState()

    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch {
            withIO { filesVM.loadAsync(context) }
            setRefreshState(RefreshContentState.Finished)
        }
    }

    BackHandler(enabled = true) {
        when {
            previewerState.visible -> {
                scope.launch {
                    previewerState.closeTransform()
                }
            }

            filesVM.selectMode.value -> {
                filesVM.exitSelectMode()
            }

            filesVM.showSearchBar.value -> {
                filesVM.exitSearchMode()
                scope.launch(Dispatchers.IO) {
                    filesVM.loadAsync(context)
                }
            }

            filesVM.showPasteBar.value -> {
                filesVM.cutFiles.clear()
                filesVM.copyFiles.clear()
                filesVM.showPasteBar.value = false
            }

            filesVM.canNavigateBack() -> {
                filesVM.navigateBack()
                scope.launch(Dispatchers.IO) {
                    filesVM.loadAsync(context)
                }
            }

            else -> {
                navController.popBackStack()
            }
        }
    }

    LaunchedEffect(fileType) {
        scope.launch(Dispatchers.IO) {
            filesVM.loadLastPathAsync(context)
            // Only override the inferred type if explicitly passed
            fileType?.let { type ->
                if (type == FilesType.APP) {
                    filesVM.root = FileSystemHelper.getExternalFilesDirPath(context)
                    filesVM.type = FilesType.APP
                    filesVM.breadcrumbs.clear()
                    filesVM.breadcrumbs.add(BreadcrumbItem(filesVM.getRootDisplayName(), filesVM.root))
                    filesVM.initPath(filesVM.root)
                }
                // Add other specific file type handling here if needed
            }
            filesVM.loadAsync(context)
            audioPlaylistVM.loadAsync(context)
        }
    }

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            when (event) {
                is PermissionsResultEvent -> {
                    scope.launch(Dispatchers.IO) {
                        filesVM.loadAsync(context)
                    }
                }

                is DrawerMenuItemClickedEvent -> {
                    val m = event.model
                    filesVM.offset = 0
                    filesVM.root = m.data as String
                    filesVM.type = when (m.iconId) {
                        R.drawable.sd_card -> FilesType.SDCARD
                        R.drawable.usb -> FilesType.USB_STORAGE
                        R.drawable.app_icon -> FilesType.APP
                        R.drawable.history -> FilesType.RECENTS
                        else -> FilesType.INTERNAL_STORAGE
                    }
                    filesVM.breadcrumbs.clear()
                    filesVM.breadcrumbs.add(BreadcrumbItem(filesVM.getRootDisplayName(), filesVM.root))
                    filesVM.initPath(filesVM.root)

                    scope.launch(Dispatchers.IO) {
                        filesVM.loadAsync(context)
                    }
                }

                is ActionEvent -> {
                    if (event.source == ActionSourceType.FILE) {
                        scope.launch(Dispatchers.IO) {
                            filesVM.loadAsync(context)
                        }
                    }
                }
            }
        }
    }

    if (filesVM.showSortDialog.value) {
        FileSortDialog(filesVM.sortBy, onSelected = {
            scope.launch(Dispatchers.IO) {
                FileSortByPreference.putAsync(context, it)
                filesVM.sortBy.value = it
                filesVM.loadAsync(context)
            }
        }, onDismiss = {
            filesVM.showSortDialog.value = false
        })
    }

    if (filesVM.showCreateFolderDialog.value) {
        val folderNameValue = remember { mutableStateOf("") }
        TextFieldDialog(
            title = stringResource(id = R.string.create_folder),
            value = folderNameValue.value,
            placeholder = stringResource(id = R.string.name),
            onValueChange = { folderNameValue.value = it },
            onDismissRequest = { filesVM.showCreateFolderDialog.value = false },
            onConfirm = { name ->
                scope.launch {
                    DialogHelper.showLoading()
                    withIO { FileSystemHelper.createDirectory(filesVM.path + "/" + name) }
                    DialogHelper.hideLoading()
                    withIO { filesVM.loadAsync(context) }
                    filesVM.showCreateFolderDialog.value = false
                }
            }
        )
    }

    if (filesVM.showCreateFileDialog.value) {
        val fileNameValue = remember { mutableStateOf("") }
        TextFieldDialog(
            title = stringResource(id = R.string.create_file),
            value = fileNameValue.value,
            placeholder = stringResource(id = R.string.name),
            onValueChange = { fileNameValue.value = it },
            onDismissRequest = { filesVM.showCreateFileDialog.value = false },
            onConfirm = { name ->
                scope.launch {
                    DialogHelper.showLoading()
                    withIO { FileSystemHelper.createFile(filesVM.path + "/" + name) }
                    DialogHelper.hideLoading()
                    withIO { filesVM.loadAsync(context) }
                    filesVM.showCreateFileDialog.value = false
                }
            }
        )
    }

    if (filesVM.showFolderKanbanDialog.value) {
        FolderKanbanDialog(
            filesVM = filesVM,
            onDismiss = {
                filesVM.showFolderKanbanDialog.value = false
            }
        )
    }

    FileInfoBottomSheet(filesVM = filesVM)

    PScaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SearchableTopBar(
                navController = navController,
                viewModel = filesVM,
                scrollBehavior = scrollBehavior,
                title = when {
                    filesVM.selectMode.value -> LocaleHelper.getStringF(
                        R.string.x_selected,
                        "count",
                        filesVM.selectedIds.size
                    )

                    filesVM.type == FilesType.RECENTS -> stringResource(R.string.recents)
                    filesVM.path != filesVM.root -> filesVM.path.getFilenameFromPath()
                    else -> stringResource(R.string.files)
                },
                subtitle = if (!filesVM.selectMode.value) {
                    val foldersCount = itemsState.count { it.isDir }
                    val filesCount = itemsState.count { !it.isDir }
                    val strList = mutableListOf<String>()
                    if (foldersCount > 0) {
                        strList.add(LocaleHelper.getQuantityString(R.plurals.x_folders, foldersCount))
                    }
                    if (filesCount > 0) {
                        strList.add(LocaleHelper.getQuantityString(R.plurals.x_files, filesCount))
                    }
                    strList.joinToString(", ")
                } else "",
                navigationIcon = {
                    if (filesVM.selectMode.value) {
                        NavigationCloseIcon {
                            filesVM.exitSelectMode()
                        }
                    } else {
                        NavigationBackIcon {
                            navController.popBackStack()
                        }
                    }
                },
                actions = {
                    if (!filesVM.selectMode.value) {
                        ActionButtonSearch {
                            filesVM.enterSearchMode()
                        }

                        ActionButtonFolderKanban {
                            filesVM.showFolderKanbanDialog.value = true
                        }

                        ActionButtonSort {
                            filesVM.showSortDialog.value = true
                        }

                        ActionButtonMoreWithMenu { dismiss ->
                            var showHiddenFiles by remember { mutableStateOf(false) }
                            LaunchedEffect(Unit) {
                                showHiddenFiles = withContext(Dispatchers.IO) {
                                    ShowHiddenFilesPreference.getAsync(context)
                                }
                            }

                            PDropdownMenuItem(
                                text = { Text(stringResource(R.string.show_hidden_files)) },
                                leadingIcon = {
                                    Checkbox(
                                        checked = showHiddenFiles,
                                        onCheckedChange = null // handle in onClick
                                    )
                                },
                                onClick = {
                                    dismiss()
                                    scope.launch(Dispatchers.IO) {
                                        val newValue = !showHiddenFiles
                                        ShowHiddenFilesPreference.putAsync(context, newValue)
                                        showHiddenFiles = newValue
                                        filesVM.loadAsync(context)
                                    }
                                }
                            )

                            PDropdownMenuItemCreateFolder {
                                dismiss()
                                filesVM.showCreateFolderDialog.value = true
                            }

                            PDropdownMenuItemCreateFile {
                                dismiss()
                                filesVM.showCreateFileDialog.value = true
                            }
                        }
                    } else {
                        PTopRightButton(
                            label = stringResource(if (filesVM.isAllSelected()) R.string.unselect_all else R.string.select_all),
                            click = {
                                filesVM.toggleSelectAll()
                            }
                        )
                        HorizontalSpace(dp = 8.dp)
                    }
                },
                onSearchAction = { query ->
                    filesVM.queryText.value = query
                    scope.launch(Dispatchers.IO) {
                        filesVM.loadAsync(context)
                    }
                }
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = filesVM.showBottomActions(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                FilesSelectModeBottomActions(
                    filesVM = filesVM,
                    onShowPasteBar = { filesVM.showPasteBar.value = it }
                )
            }

            AnimatedVisibility(
                visible = filesVM.showPasteBar.value,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                FilePasteBar(
                    filesVM = filesVM,
                    coroutineScope = scope,
                    onPasteComplete = {
                        scope.launch(Dispatchers.IO) {
                            filesVM.loadAsync(context)
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (Permission.WRITE_EXTERNAL_STORAGE.can(context) && filesVM.type != FilesType.RECENTS) {
                BreadcrumbView(
                    breadcrumbs = filesVM.breadcrumbs,
                    selectedIndex = filesVM.selectedBreadcrumbIndex.value,
                    onItemClick = { item ->
                        filesVM.navigateToDirectory(context, item.path)
                    }
                )
            }

            PullToRefresh(
                refreshLayoutState = topRefreshLayoutState,
            ) {
                FileListContent(
                    navController = navController,
                    filesVM = filesVM,
                    files = itemsState,
                    loadFiles = { _, _ ->
                        scope.launch(Dispatchers.IO) {
                            filesVM.loadAsync(context)
                        }
                    },
                    previewerState = previewerState,
                    audioPlaylistVM = audioPlaylistVM
                )
            }
        }
    }

    MediaPreviewer(state = previewerState)
}

