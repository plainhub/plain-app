package com.ismartcoding.plain.ui.videos

import com.ismartcoding.plain.preferences.PrefKeys

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.platform.media.MediaPreviewer
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.ui.platform.system.isGestureInteractionMode
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.ui.base.AnimatedBottomAction
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.components.NeedPermissionColumn
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.dragselect.gridDragSelect
import com.ismartcoding.plain.ui.base.dragselect.rememberDragSelectState
import com.ismartcoding.plain.ui.base.fastscroll.LazyVerticalGridScrollbar
import com.ismartcoding.plain.ui.components.pinchZoomGrid
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.components.rememberBoostFlingBehavior
import com.ismartcoding.plain.ui.media.MediaDateGroupHeader
import com.ismartcoding.plain.ui.components.MediaFilesSelectModeBottomActions
import com.ismartcoding.plain.ui.media.MediaTopBar
import com.ismartcoding.plain.ui.media.MediaPermissionEffects
import com.ismartcoding.plain.ui.media.VideoGridItem
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosPage(
    navController: NavHostController,
    initialLoadGate: NavLoadGate? = null,
    source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.ui.navigation.LocalContentSource.current,
    videosVM: VideosViewModel = viewModel(key = "videosVM") { VideosViewModel(source) },
    tagsVM: TagsViewModel = viewModel(key = "videoTagsVM") { TagsViewModel(source.tags) },
    mediaFoldersVM: MediaFoldersViewModel = viewModel(key = "videoFoldersVM") { MediaFoldersViewModel(source) },
    castVM: CastViewModel = viewModel(key = "videoCastVM") { CastViewModel(source) },
) {
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        mediaFoldersVM.dataType.value = videosVM.dataType
        tagsVM.dataType.value = videosVM.dataType
    }
    val tagsState by tagsVM.itemsFlow.collectAsState()
    val itemsState by videosVM.itemsFlow.collectAsState()
    val scrollState = rememberLazyGridState()
    videosVM.scrollStateMap[0] = scrollState
    val dragSelectState = rememberDragSelectState({ scrollState })
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = {
        scrollState.firstVisibleItemIndex > 0 && !dragSelectState.selectMode
    })
    val previewerState = rememberPreviewerState()
    val tagsMapState by tagsVM.tagsMapFlow.collectAsState()
    val bucketsMap by mediaFoldersVM.bucketsMapFlow.collectAsState()
    val cellsPerRow = remember { mutableIntStateOf(Prefs.videoGridCellsPerRow.value) }
    val windowInfo = LocalWindowInfo.current
    val imageWidthPx = remember(cellsPerRow.value, windowInfo.containerSize.width) {
        with(density) {
            ((windowInfo.containerSize.width.toDp() - ((cellsPerRow.value - 1) * 2).dp) / cellsPerRow.value).toPx().toInt()
        }
    }
    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch {
            videosVM.loadAsync(tagsVM)
            withIO { mediaFoldersVM.loadAsync() }
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
                scope.launch(Dispatchers.Default) { videosVM.loadAsync(tagsVM) }
            }
        }
    }

    val reloadAfterGrant: () -> Unit = {
        scope.launch(Dispatchers.Default) {
            cellsPerRow.value = Prefs.videoGridCellsPerRow.value
            videosVM.sortBy.value = Prefs.videoSortBy.value
            videosVM.loadAsync(tagsVM)
            mediaFoldersVM.loadAsync()
        }
    }

    MediaPermissionEffects(videosVM.hasPermission, initialLoadGate, reloadAfterGrant, videosVM.source)
    LaunchedEffect(dragSelectState.selectMode, (previewerState.visible && !isGestureInteractionMode())) {
        if (dragSelectState.selectMode || (previewerState.visible && !isGestureInteractionMode())) scrollBehavior.reset()
    }

    ViewVideoBottomSheet(videosVM, tagsVM, tagsMapState, tagsState, dragSelectState)
    if (videosVM.showTagsDialog.value) {
        TagsBottomSheet(tagsVM) { videosVM.showTagsDialog.value = false }
    }
    CastDialog(castVM)

    // Date grouping is computed outside the grid DSL: the DSL block re-executes
    // on every recomposition (tags/buckets landing right after items), which
    // would re-run the O(n) grouping each time.
    val isGroupMode = videosVM.sortBy.value == FileSortBy.TAKEN_AT_DESC
            && videosVM.queryText.value.isEmpty()
    val groupedItems = remember(itemsState, isGroupMode) {
        if (isGroupMode) groupMediaByDate(itemsState, { it.takenAt ?: it.createdAt }) else emptyList()
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
                    scope.launch(Dispatchers.Default) {
                        Prefs.set(PrefKeys.VIDEO_SORT_BY, sortBy.name)
                        videosVM.sortBy.value = sortBy
                        videosVM.loadAsync(tagsVM)
                    }
                },
                onSearchAction = { tv -> scope.launch(Dispatchers.Default) { videosVM.loadAsync(tv) } },
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
                NeedPermissionColumn(UiRes.drawable.ui_drawable_video, AppFeatureType.FILES.getPermission()!!); return@Column
            }

            PullToRefresh(refreshLayoutState = topRefreshLayoutState) {
                AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                    if (itemsState.isNotEmpty()) {
                        val flingBehavior = rememberBoostFlingBehavior(cellsPerRow.value / 3f)
                        LazyVerticalGridScrollbar(state = scrollState) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(cellsPerRow.value),
                                state = scrollState,
                                flingBehavior = flingBehavior,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                                    .gridDragSelect(items = itemsState, state = dragSelectState)
                                    .pinchZoomGrid(cellsPerRow = cellsPerRow, hapticFeedback = hapticFeedback, scope = scope) {
                                        Prefs.set(PrefKeys.VIDEO_GRID_CELLS_PER_ROW, it)
                                    },
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                if (isGroupMode) {
                                    groupedItems.forEach { group ->
                                        item(span = { GridItemSpan(maxLineSpan) }, key = "header_${group.dateKey}", contentType = "header") {
                                            val ids = group.items.map { it.id }
                                            val allSelected = ids.isNotEmpty() && ids.all { dragSelectState.isSelected(it) }
                                            MediaDateGroupHeader(
                                                dateLabel = group.dateLabel,
                                                allSelected = allSelected,
                                                partialSelected = !allSelected && ids.any { dragSelectState.isSelected(it) },
                                                showCheckbox = dragSelectState.selectMode,
                                            ) {
                                                if (!dragSelectState.selectMode) dragSelectState.enterSelectMode()
                                                if (allSelected) dragSelectState.removeSelectedIds(ids) else dragSelectState.addSelectedIds(ids)
                                            }
                                        }
                                        items(group.items, key = { it.id }, contentType = { "video" }, span = { GridItemSpan(1) }) { m ->
                                            VideoGridItem(
                                                modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                                                videosVM,
                                                castVM,
                                                m,
                                                showSize = cellsPerRow.value < 6,
                                                previewerState,
                                                dragSelectState,
                                                imageWidthPx,
                                                sort = videosVM.sortBy.value,
                                            )
                                        }
                                    }
                                } else {
                                    items(itemsState, key = { it.id }, contentType = { "video" }, span = { GridItemSpan(1) }) { m ->
                                        VideoGridItem(
                                            modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null),
                                            videosVM,
                                            castVM,
                                            m,
                                            showSize = cellsPerRow.value < 6,
                                            previewerState,
                                            dragSelectState,
                                            imageWidthPx,
                                            sort = videosVM.sortBy.value,
                                        )
                                    }
                                }
                                item(span = { GridItemSpan(maxLineSpan) }, key = "loadMore") {
                                    if (itemsState.isNotEmpty() && !videosVM.noMore.value) {
                                        LaunchedEffect(Unit) { scope.launch(Dispatchers.Default) { videosVM.moreAsync(tagsVM) } }
                                    }
                                    LoadMoreRefreshContent(videosVM.noMore.value)
                                }
                                item(span = { GridItemSpan(maxLineSpan) }, key = "bottomSpace") { BottomSpace(paddingValues) }
                            }
                        }
                    } else {
                        NoDataColumn(loading = videosVM.showLoading.value, search = videosVM.showSearchBar.value)
                    }
                }
            }
        }
    }

    MediaPreviewer(
        state = previewerState,
        tagsVM = tagsVM,
        tagsMap = tagsMapState,
        tagsState = tagsState,
        onRenamed = { scope.launch(Dispatchers.Default) { videosVM.loadAsync(tagsVM) } },
        deleteAction = { item ->
            scope.launch(Dispatchers.Default) {
                videosVM.delete(tagsVM, setOf(item.mediaId))
                previewerState.closeTransform()
            }
        },
        onTagsChanged = {},
    )
}
