package com.ismartcoding.plain.ui.videos

import com.ismartcoding.plain.ui.media.previewTrashActions
import com.ismartcoding.plain.preferences.PrefKeys

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.lazy.layout.LazyLayoutCacheWindow
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.models.VideoSortBy
import com.ismartcoding.plain.ui.platform.media.MediaPreviewer
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.ui.platform.system.isGestureInteractionMode
import com.ismartcoding.plain.Prefs
import coil3.SingletonImageLoader
import coil3.compose.LocalPlatformContext
import com.ismartcoding.plain.rust.graphql.ContentSource
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
import com.ismartcoding.plain.ui.components.MediaFilesSelectModeBottomActions
import com.ismartcoding.plain.ui.media.MediaTopBar
import com.ismartcoding.plain.ui.media.MediaPermissionEffects
import com.ismartcoding.plain.ui.media.VideoGridItem
import com.ismartcoding.plain.ui.media.MediaGrid
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
import kotlinx.coroutines.Dispatchers
import com.ismartcoding.plain.ui.state.launchSafe
import kotlinx.coroutines.launch
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.video as ui_drawable_video

@OptIn(ExperimentalFoundationApi::class)
internal val remoteVideosCacheWindow = LazyLayoutCacheWindow(aheadFraction = 0.25f, behindFraction = 0f)

@Composable
private fun videosViewModel(source: ContentSource): VideosViewModel {
    val cache = if (source is ContentSource.Remote) {
        SingletonImageLoader.get(LocalPlatformContext.current).diskCache?.let(::VideoGridCache)
    } else null
    return viewModel(key = "videosVM") { VideosViewModel(source, cache) }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun VideosPage(
    navController: NavHostController,
    initialLoadGate: NavLoadGate? = null,
    source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.ui.navigation.LocalContentSource.current,
    videosVM: VideosViewModel = videosViewModel(source),
    tagsVM: TagsViewModel = viewModel(key = "videoTagsVM") { TagsViewModel(source.tags) },
    mediaFoldersVM: MediaFoldersViewModel = viewModel(key = "videoFoldersVM") { MediaFoldersViewModel(source) },
    castVM: CastViewModel = viewModel(key = "videoCastVM") { CastViewModel(source) },
) {
    val scope = rememberCoroutineScope()
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        mediaFoldersVM.dataType.value = videosVM.dataType
        tagsVM.dataType.value = videosVM.dataType
    }
    val tagsState by tagsVM.itemsFlow.collectAsState()
    val itemsState by videosVM.itemsFlow.collectAsState()
    val scrollState = if (videosVM.source is com.ismartcoding.plain.rust.graphql.ContentSource.Remote) {
        rememberLazyGridState(cacheWindow = remoteVideosCacheWindow)
    } else {
        rememberLazyGridState()
    }
    val cellsPerRow = remember { mutableIntStateOf(Prefs.videoGridCellsPerRow.value) }
    val thumbnailRequests = remember(videosVM, scrollState) {
        if (videosVM.source is ContentSource.Remote) VideoGridThumbnailRequests() else null
    }
    LaunchedEffect(scrollState, thumbnailRequests) {
        if (thumbnailRequests != null) {
            try {
                snapshotFlow {
                    val ids = scrollState.layoutInfo.visibleItemsInfo.mapNotNull {
                        if (it.contentType == "video") it.key as String else null
                    }
                    val direction = when {
                        !scrollState.isScrollInProgress -> null
                        scrollState.lastScrolledForward -> true
                        scrollState.lastScrolledBackward -> false
                        else -> null
                    }
                    Triple(ids, cellsPerRow.value, direction)
                }.collect { (ids, columns, direction) ->
                    thumbnailRequests.updateVisible(ids, columns, direction)
                }
            } finally {
                thumbnailRequests.updateVisible(emptyList())
            }
        }
    }
    val dragSelectState = rememberDragSelectState(scrollState)
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = {
        scrollState.firstVisibleItemIndex > 0 && !dragSelectState.selectMode
    })
    val previewerState = rememberPreviewerState()
    val tagsMapState by tagsVM.tagsMapFlow.collectAsState()
    val bucketsMap by mediaFoldersVM.bucketsMapFlow.collectAsState()
    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launchSafe(
            onError = { error ->
                setRefreshState(RefreshContentState.Failed)
                ToastManager.showErrorToast(error.message ?: error.toString())
            },
            onDone = { videosVM.showLoading.value = false },
        ) {
            videosVM.loadAsync(tagsVM)
            mediaFoldersVM.loadAsync()
            setRefreshState(RefreshContentState.Finished)
        }
    }

    PBackHandler(enabled = previewerState.visible || dragSelectState.selectMode || castVM.castMode.value || videosVM.showSearchBar.value) {
        when {
            previewerState.visible -> scope.launch { previewerState.closeTransform() }
            dragSelectState.selectMode -> dragSelectState.exitSelectMode()
            castVM.castMode.value -> castVM.exitCastMode()
            videosVM.showSearchBar.value && (!videosVM.searchActive.value || videosVM.queryText.value.isEmpty()) -> {
                videosVM.exitSearchMode(); videosVM.showLoading.value = true
                scope.launchSafe(onDone = { videosVM.showLoading.value = false }) { videosVM.loadAsync(tagsVM) }
            }
        }
    }

    val reloadAfterGrant: () -> Unit = {
        cellsPerRow.value = Prefs.videoGridCellsPerRow.value
        videosVM.sortBy.value = Prefs.videoSortBy.value
        videosVM.enterPage(scope, tagsVM, mediaFoldersVM::loadAsync)
    }

    MediaPermissionEffects(videosVM.hasPermission, initialLoadGate, reloadAfterGrant, videosVM.source, feature = videosVM.accessFeature)
    LaunchedEffect(dragSelectState.selectMode, (previewerState.visible && !isGestureInteractionMode())) {
        if (dragSelectState.selectMode || (previewerState.visible && !isGestureInteractionMode())) scrollBehavior.reset()
    }

    ViewVideoBottomSheet(videosVM, tagsVM, tagsMapState, tagsState, dragSelectState)
    if (videosVM.showTagsDialog.value) {
        TagsBottomSheet(tagsVM) { videosVM.showTagsDialog.value = false }
    }
    CastDialog(castVM)

    val isGroupMode = videosVM.sortBy.value == VideoSortBy.TAKEN_AT_DESC
            && videosVM.queryText.value.isEmpty()
    val groupedItems = remember(itemsState, isGroupMode) {
        if (isGroupMode) groupMediaByDate(itemsState, { it.takenAt ?: it.createdAt }) else null
    }

    MediaTopBar(
        navController = navController,
        mediaVM = videosVM,
        tagsVM = tagsVM,
        castVM = castVM,
        mediaFoldersVM = mediaFoldersVM,
        dragSelectState = dragSelectState,
                scrollBehavior = scrollBehavior,
                bucketsMap = bucketsMap,
                itemsState = itemsState,
                scrollToTop = { scope.launch { scrollState.scrollToItem(0) } },
                onSortSelected = { sortBy ->
                    scope.launchSafe(onDone = { videosVM.showLoading.value = false }) {
                        Prefs.set(PrefKeys.VIDEO_SORT_BY, sortBy.name)
                        videosVM.sortBy.value = sortBy
                        videosVM.loadAsync(tagsVM)
                    }
                },
                onSearchAction = { tv -> scope.launchSafe(onDone = { videosVM.showLoading.value = false }) { videosVM.loadAsync(tv) } },
        bottomBar = {
            AnimatedBottomAction(visible = dragSelectState.showBottomActions()) {
                MediaFilesSelectModeBottomActions(
                    vm = videosVM,
                    tagsVM = tagsVM,
                    tagsState = tagsState,
                    dragSelectState = dragSelectState,
                    getCollectableItems = { videosVM.itemsFlow.collectAsStateValue() },
                    isInTrashMode = videosVM.trash.value,
                )
            }
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            if (!videosVM.hasPermission.value) {
                NeedPermissionColumn(UiRes.drawable.ui_drawable_video, AppFeatureType.FILES.getPermission()!!, feature = videosVM.accessFeature); return@Column
            }

            PullToRefresh(refreshLayoutState = topRefreshLayoutState) {
                if (itemsState.isEmpty()) {
                    NoDataColumn(loading = videosVM.showLoading.value, search = videosVM.showSearchBar.value)
                    return@PullToRefresh
                }

                MediaGrid(
                    rows = itemsState,
                    groups = groupedItems,
                    contentType = "video",
                    columns = cellsPerRow.value,
                    state = scrollState,
                    selection = dragSelectState,
                    noMore = videosVM.noMore,
                    padding = paddingValues,
                    onLoadMore = { scope.launchSafe { videosVM.moreAsync(tagsVM) } },
                    modifier = Modifier
                        .fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .gridDragSelect(items = itemsState, state = dragSelectState)
                        .pinchZoomGrid(cellsPerRow = cellsPerRow, hapticFeedback = hapticFeedback, scope = scope) {
                            Prefs.set(PrefKeys.VIDEO_GRID_CELLS_PER_ROW, it)
                        },
                ) { item ->
                    VideoGridItem(
                        modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                        videosVM = videosVM,
                        castVM = castVM,
                        m = item,
                        showSize = cellsPerRow.value < 6,
                        previewerState = previewerState,
                        dragSelectState = dragSelectState,
                        sort = videosVM.sortBy.value,
                        thumbnailRequests = thumbnailRequests,
                    )
                }
            }
        }
    }

    MediaPreviewer(
        state = previewerState,
        preloadAdjacentVideos = true,
        tagsVM = tagsVM,
        tagsMap = tagsMapState,
        tagsState = tagsState,
        onRenamed = { scope.launchSafe(onDone = { videosVM.showLoading.value = false }) { videosVM.loadAsync(tagsVM) } },
        trashActions = videosVM.previewTrashActions(tagsVM) {
            scope.launch { previewerState.closeTransform() }
        },
        onTagsChanged = {},
    )
}
