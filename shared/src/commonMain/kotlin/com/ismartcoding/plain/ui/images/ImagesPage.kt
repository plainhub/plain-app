package com.ismartcoding.plain.ui.images

import com.ismartcoding.plain.preferences.PrefKeys
import com.ismartcoding.plain.platform.permissions.imageLibraryPermission
import com.ismartcoding.plain.rust.graphql.ContentSource

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.models.ImageSortBy
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.ui.platform.system.isGestureInteractionMode
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.ui.base.AnimatedBottomAction
import com.ismartcoding.plain.ui.components.NeedPermissionColumn
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.dragselect.gridDragSelect
import com.ismartcoding.plain.ui.base.dragselect.rememberDragSelectState
import com.ismartcoding.plain.ui.components.pinchZoomGrid
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.components.ToastManager
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.media.MediaPermissionEffects
import com.ismartcoding.plain.ui.media.ImageGridItem
import com.ismartcoding.plain.ui.media.MediaGrid
import com.ismartcoding.plain.ui.components.MediaFilesSelectModeBottomActions
import com.ismartcoding.plain.ui.media.MediaTopBar
import com.ismartcoding.plain.ui.media.viewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.media.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.media.extensions.reset
import com.ismartcoding.plain.ui.media.groupMediaByDate
import com.ismartcoding.plain.ui.casting.CastViewModel
import com.ismartcoding.plain.ui.files.MediaFoldersViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.state.exitSearchMode
import com.ismartcoding.plain.ui.navigation.NavLoadGate
import com.ismartcoding.plain.ui.casting.CastDialog
import com.ismartcoding.plain.ui.tags.TagsBottomSheet
import com.ismartcoding.plain.ui.state.launchSafe
import kotlinx.coroutines.launch
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.image as ui_drawable_image

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagesPage(
    navController: NavHostController,
    initialLoadGate: NavLoadGate? = null,
    source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.ui.navigation.LocalContentSource.current,
    imagesVM: ImagesViewModel = viewModel(key = "imagesVM") { ImagesViewModel(source) },
    tagsVM: TagsViewModel = viewModel(key = "imageTagsVM") { TagsViewModel(source.tags) },
    mediaFoldersVM: MediaFoldersViewModel = viewModel(key = "imageFoldersVM") { MediaFoldersViewModel(source) },
    castVM: CastViewModel = viewModel(key = "imageCastVM") { CastViewModel(source) },
) {
    val scope = rememberCoroutineScope()
    tagsVM.dataType.value = imagesVM.dataType
    mediaFoldersVM.dataType.value = imagesVM.dataType
    val tagsState by tagsVM.itemsFlow.collectAsState()
    val hapticFeedback = LocalHapticFeedback.current
    val itemsState by imagesVM.itemsFlow.collectAsState()
    val scrollState = rememberLazyGridState()
    val dragSelectState = rememberDragSelectState(scrollState)
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = {
        scrollState.firstVisibleItemIndex > 0 && !dragSelectState.selectMode
    })
    val previewerState = rememberPreviewerState()
    val tagsMapState by tagsVM.tagsMapFlow.collectAsState()
    val bucketsMap by mediaFoldersVM.bucketsMapFlow.collectAsState()
    val cellsPerRow = remember { mutableIntStateOf(Prefs.imageGridCellsPerRow.value) }
    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launchSafe(
            onError = { error ->
                setRefreshState(RefreshContentState.Failed)
                ToastManager.showErrorToast(error.message ?: error.toString())
            },
            onDone = { imagesVM.showLoading.value = false },
        ) {
            imagesVM.loadAsync(tagsVM)
            mediaFoldersVM.loadAsync()
            setRefreshState(RefreshContentState.Finished)
        }
    }

    PBackHandler(enabled = previewerState.visible || dragSelectState.selectMode || castVM.castMode.value || imagesVM.showSearchBar.value) {
        when {
            previewerState.visible -> scope.launch { previewerState.closeTransform() }
            dragSelectState.selectMode -> dragSelectState.exitSelectMode()
            castVM.castMode.value -> castVM.exitCastMode()
            imagesVM.showSearchBar.value && (!imagesVM.searchActive.value || imagesVM.queryText.value.isEmpty()) -> {
                imagesVM.exitSearchMode()
                imagesVM.showLoading.value = true
                scope.launchSafe(onDone = { imagesVM.showLoading.value = false }) {
                    imagesVM.loadAsync(tagsVM)
                }
            }
        }
    }

    val reloadAfterGrant: () -> Unit = {
        cellsPerRow.value = Prefs.imageGridCellsPerRow.value
        imagesVM.sortBy.value = Prefs.imageSortBy.value
        imagesVM.loadFromPage(scope, tagsVM, mediaFoldersVM::loadAsync)
    }

    MediaPermissionEffects(imagesVM.hasPermission, initialLoadGate, reloadAfterGrant, imagesVM.source, imagesVM.accessFeature, imageLibraryPermission())
    LaunchedEffect(dragSelectState.selectMode, (previewerState.visible && !isGestureInteractionMode())) {
        if (dragSelectState.selectMode || (previewerState.visible && !isGestureInteractionMode())) {
            scrollBehavior.reset()
        }
    }

    ViewImageBottomSheet(imagesVM, tagsVM, tagsMapState, tagsState, dragSelectState)

    if (imagesVM.showTagsDialog.value) {
        TagsBottomSheet(tagsVM) {
            imagesVM.showTagsDialog.value = false
        }
    }

    CastDialog(castVM)

    val isGroupMode = imagesVM.sortBy.value == ImageSortBy.TAKEN_AT_DESC
            && imagesVM.queryText.value.isEmpty()
    val groupedItems = remember(itemsState, isGroupMode) {
        if (isGroupMode) groupMediaByDate(itemsState, { it.takenAt ?: it.createdAt }) else null
    }

    MediaTopBar(
        navController = navController,
        mediaVM = imagesVM,
        tagsVM = tagsVM,
        castVM = castVM,
        mediaFoldersVM = mediaFoldersVM,
        dragSelectState = dragSelectState,
                scrollBehavior = scrollBehavior,
                bucketsMap = bucketsMap,
                itemsState = itemsState,
                scrollToTop = {
                    scope.launch {
                        scrollState.scrollToItem(0)
                    }
                },
                onSortSelected = { sortBy ->
                    scope.launchSafe(onDone = { imagesVM.showLoading.value = false }) {
                        Prefs.set(PrefKeys.IMAGE_SORT_BY, sortBy.name)
                        imagesVM.sortBy.value = sortBy
                        imagesVM.loadAsync(tagsVM)
                    }
                },
                onSearchAction = { tv ->
                    scope.launchSafe(onDone = { imagesVM.showLoading.value = false }) {
                        imagesVM.loadAsync(tv)
                    }
                },
        bottomBar = {
            AnimatedBottomAction(visible = dragSelectState.showBottomActions()) {
                MediaFilesSelectModeBottomActions(
                    vm = imagesVM,
                    tagsVM = tagsVM,
                    tagsState = tagsState,
                    dragSelectState = dragSelectState,
                    getCollectableItems = { imagesVM.itemsFlow.collectAsStateValue() },
                    isInTrashMode = imagesVM.trash.value
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            if (!imagesVM.hasPermission.value) {
                NeedPermissionColumn(UiRes.drawable.ui_drawable_image, if (source is ContentSource.Remote) AppFeatureType.FILES.getPermission()!! else imageLibraryPermission(), feature = imagesVM.accessFeature); return@Column
            }

            PullToRefresh(refreshLayoutState = topRefreshLayoutState) {
                if (itemsState.isEmpty()) {
                    NoDataColumn(loading = imagesVM.showLoading.value, search = imagesVM.showSearchBar.value)
                    return@PullToRefresh
                }

                MediaGrid(
                    rows = itemsState,
                    groups = groupedItems,
                    contentType = "image",
                    columns = cellsPerRow.value,
                    state = scrollState,
                    selection = dragSelectState,
                    noMore = imagesVM.noMore,
                    padding = paddingValues,
                    onLoadMore = { scope.launchSafe { imagesVM.moreAsync(tagsVM) } },
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .gridDragSelect(items = itemsState, state = dragSelectState)
                        .pinchZoomGrid(cellsPerRow = cellsPerRow, hapticFeedback = hapticFeedback, scope = scope) {
                            Prefs.set(PrefKeys.IMAGE_GRID_CELLS_PER_ROW, it)
                        },
                ) { item ->
                    ImageGridItem(
                        scope = scope,
                        modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                        imagesVM = imagesVM,
                        castVM = castVM,
                        m = item,
                        showSize = cellsPerRow.value < 6,
                        previewerState = previewerState,
                        dragSelectState = dragSelectState,
                    )
                }
            }
        }
    }

    ImagesPagePreviewer(imagesVM, tagsVM, previewerState, tagsMapState, tagsState)
}
