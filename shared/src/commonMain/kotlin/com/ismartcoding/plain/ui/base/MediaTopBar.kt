package com.ismartcoding.plain.ui.base

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.db.IData
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.components.SortAndBrowseDialog
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.BaseMediaViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.enterSearchMode
import com.ismartcoding.plain.ui.page.home.MediaSidebarDrawer
import kotlinx.coroutines.launch

/**
 * Shared layout for the media pages (Images / Audio / Videos / Docs). Wraps the
 * page in a [ModalNavigationDrawer] hosting the folder picker, and renders the top
 * bar with a search button plus a WeChat-mini-program style capsule whose "more"
 * button opens a bottom sheet with sort / cast / tags. The separate Folders button
 * is removed — folders now live in the navigation drawer.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun <T : IData> MediaTopBar(
    navController: NavHostController,
    mediaVM: BaseMediaViewModel<T>,
    tagsVM: TagsViewModel,
    castVM: CastViewModel,
    mediaFoldersVM: MediaFoldersViewModel,
    dragSelectState: DragSelectState,
    scrollBehavior: TopAppBarScrollBehavior,
    bucketsMap: Map<String, DMediaBucket>,
    itemsState: List<T>,
    scrollToTop: () -> Unit,
    onSortSelected: (sortBy: FileSortBy) -> Unit = {},
    onSearchAction: (tagsViewModel: TagsViewModel) -> Unit,
    bottomBar: (@Composable () -> Unit)? = null,
    /** Replaces the default leading icon (folder drawer) — e.g. a back arrow on sub-pages. */
    navigationIcon: (@Composable () -> Unit)? = null,
    /** Replaces the default trailing actions (search + more/close capsule) on sub-pages. */
    topBarActions: (@Composable () -> Unit)? = null,
    content: @Composable (PaddingValues) -> Unit,
) {
    val isDocs = mediaVM.dataType == DataType.DOC
    val scope = rememberCoroutineScope()
    val drawerState = rememberDrawerState(DrawerValue.Closed)

    val title = getMediaPageTitle(
        mediaType = mediaVM.dataType,
        isCastMode = castVM.castMode.value,
        castDeviceName = CastPlayer.currentDevice?.description?.device?.friendlyName,
        bucket = bucketsMap[mediaVM.bucketId.value],
        dragSelectState = dragSelectState,
        tagName = mediaVM.tag.value?.name,
    )
    val containerColor = if (castVM.castMode.value) MaterialTheme.colorScheme.secondaryContainer else null

    val defaultCapsule: @Composable () -> Unit = {
        PCapsuleMoreClose(
            onClose = { navController.navigateUp() },
        ) { dismiss ->
            PSheetActionRow(Res.drawable.sort, stringResource(Res.string.sort)) {
                dismiss()
                mediaVM.showSortAndBrowseDialog.value = true
            }
            if (!isDocs) {
                PSheetActionRow(Res.drawable.cast, stringResource(Res.string.cast_mode)) {
                    dismiss()
                    castVM.showCastDialog.value = true
                }
            }
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                MediaSidebarDrawer(
                    mediaVM = mediaVM,
                    mediaFoldersVM = mediaFoldersVM,
                    tagsVM = tagsVM,
                    drawerState = drawerState,
                )
            }
        },
    ) {
        Box(Modifier.fillMaxSize()) {
            PScaffold(
                topBar = {
                    SearchableTopBar(
                        navController = navController,
                        viewModel = mediaVM,
                        scrollBehavior = scrollBehavior,
                        title = title,
                        containerColor = containerColor,
                        scrollToTop = scrollToTop,
                        navigationIcon = navigationIcon ?: @Composable {
                            if (dragSelectState.selectMode) {
                                NavigationCloseIcon {
                                    dragSelectState.exitSelectMode()
                                }
                            } else if (castVM.castMode.value) {
                                NavigationCloseIcon {
                                    castVM.exitCastMode()
                                }
                            } else {
                                PIconButton(
                                    icon = Res.drawable.left_panel_open,
                                    contentDescription = stringResource(Res.string.folders),
                                    click = {
                                        scope.launch {
                                            if (drawerState.isOpen) drawerState.close() else drawerState.open()
                                        }
                                    }
                                )
                            }
                        },
                        actions = {
                            if (!mediaVM.hasPermission.value) {
                                // Data-dependent actions are useless while gated,
                                // but keep the capsule available for closing the page.
                                defaultCapsule()
                                return@SearchableTopBar
                            }
                            if (castVM.castMode.value) {
                                return@SearchableTopBar
                            }
                            if (dragSelectState.selectMode) {
                                PTopRightButton(
                                    label = stringResource(if (dragSelectState.isAllSelected(itemsState)) Res.string.unselect_all else Res.string.select_all),
                                    click = {
                                        dragSelectState.toggleSelectAll(itemsState)
                                    },
                                )
                                HorizontalSpace(dp = 8.dp)
                            } else topBarActions?.let {
                                it()
                            } ?: run {
                                ActionButtonSearch {
                                    mediaVM.enterSearchMode()
                                }
                                defaultCapsule()
                            }
                        },
                        onSearchAction = {
                            mediaVM.showLoading.value = true
                            onSearchAction(tagsVM)
                        }
                    )
                },
                bottomBar = { bottomBar?.invoke() },
            ) { paddingValues ->
                CompositionLocalProvider(LocalDrawerState provides drawerState) {
                    content(paddingValues)
                }
            }
            DrawerEdgeSwipeStrip(drawerState)
        }
    }

    if (mediaVM.showSortAndBrowseDialog.value) {
        SortAndBrowseDialog(
            mediaVM = mediaVM,
            tagsVM = tagsVM,
            sortByEntries = if (setOf(DataType.IMAGE, DataType.VIDEO).contains(mediaVM.dataType)) FileSortBy.entries else FileSortBy.entries.filter { it != FileSortBy.TAKEN_AT_DESC },
            onSortSelected = { sortBy -> onSortSelected(sortBy) },
            onDismiss = { mediaVM.showSortAndBrowseDialog.value = false },
        )
    }
}
