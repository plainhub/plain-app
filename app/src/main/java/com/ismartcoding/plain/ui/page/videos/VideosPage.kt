package com.ismartcoding.plain.ui.page.videos

import com.ismartcoding.plain.i18n.*
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.lib.channel.Channel
import com.ismartcoding.plain.lib.extensions.isGestureInteractionMode
import com.ismartcoding.plain.preferences.VideoGridCellsPerRowPreference
import com.ismartcoding.plain.preferences.VideoSortByPreference
import com.ismartcoding.plain.ui.base.AnimatedBottomAction
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.MediaTopBar
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NeedPermissionColumn
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PScrollableTabRow
import com.ismartcoding.plain.ui.base.dragselect.gridDragSelect
import com.ismartcoding.plain.ui.base.dragselect.rememberDragSelectState
import com.ismartcoding.plain.ui.base.fastscroll.LazyVerticalGridScrollbar
import com.ismartcoding.plain.ui.base.pinchZoomGrid
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.base.rememberBoostFlingBehavior
import com.ismartcoding.plain.ui.components.MediaFilesSelectModeBottomActions
import com.ismartcoding.plain.ui.components.VideoGridItem
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewer
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.extensions.reset
import com.ismartcoding.plain.ui.helpers.groupMediaByDate
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VTabData
import com.ismartcoding.plain.ui.models.VideosViewModel
import com.ismartcoding.plain.ui.models.exitSearchMode
import com.ismartcoding.plain.ui.page.cast.CastDialog
import com.ismartcoding.plain.ui.page.home.MediaFoldersBottomSheet
import com.ismartcoding.plain.ui.page.tags.TagsBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VideosPage(
    navController: NavHostController,
    videosVM: VideosViewModel = viewModel(key = "videosVM"),
    tagsVM: TagsViewModel = viewModel(key = "videoTagsVM"),
    mediaFoldersVM: MediaFoldersViewModel = viewModel(key = "videoFoldersVM"),
    castVM: CastViewModel = viewModel(key = "videoCastVM"),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val hapticFeedback = LocalHapticFeedback.current

    LaunchedEffect(Unit) {
        mediaFoldersVM.dataType.value = videosVM.dataType
        tagsVM.dataType.value = videosVM.dataType
    }
    val tagsState by tagsVM.itemsFlow.collectAsState()
    val pagerState = rememberPagerState(pageCount = {
        tagsState.size + if (AppFeatureType.MEDIA_TRASH.has()) 2 else 1
    })
    val itemsState by videosVM.itemsFlow.collectAsState()
    val dragSelectState = rememberDragSelectState({
        videosVM.scrollStateMap[pagerState.currentPage]
    })
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = {
        (videosVM.scrollStateMap[pagerState.currentPage]?.firstVisibleItemIndex ?: 0) > 0 && !dragSelectState.selectMode
    })
    val previewerState = rememberPreviewerState()
    val tagsMapState by tagsVM.tagsMapFlow.collectAsState()
    val bucketsMap by mediaFoldersVM.bucketsMapFlow.collectAsState()
    val cellsPerRow = remember { mutableIntStateOf(VideoGridCellsPerRowPreference.default) }
    val isFirstTime = remember { mutableStateOf(true) }
    val imageWidthPx = remember(cellsPerRow.value) { density.run { ((configuration.screenWidthDp.dp - ((cellsPerRow.value - 1) * 2).dp) / cellsPerRow.value).toPx().toInt() } }
    val tabs = remember(tagsState, videosVM.total.intValue, videosVM.totalTrash.intValue) {
        val baseTabs = mutableListOf(VTabData(LocaleHelper.getString(Res.string.all), "all", videosVM.total.intValue))
        if (AppFeatureType.MEDIA_TRASH.has()) baseTabs.add(VTabData(LocaleHelper.getString(Res.string.trash), "trash", videosVM.totalTrash.intValue))
        baseTabs.addAll(tagsState.map { VTabData(it.name, it.id, it.count) })
        baseTabs
    }
    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch {
            videosVM.loadAsync(context, tagsVM)
            withIO { mediaFoldersVM.loadAsync(context) }
            setRefreshState(RefreshContentState.Finished)
        }
    }

    BackHandler(enabled = previewerState.visible || dragSelectState.selectMode || castVM.castMode.value || videosVM.showSearchBar.value) {
        when {
            previewerState.visible -> scope.launch { previewerState.closeTransform() }
            dragSelectState.selectMode -> dragSelectState.exitSelectMode()
            castVM.castMode.value -> castVM.exitCastMode()
            videosVM.showSearchBar.value && (!videosVM.searchActive.value || videosVM.queryText.value.isEmpty()) -> {
                videosVM.exitSearchMode(); videosVM.showLoading.value = true
                scope.launch(Dispatchers.IO) { videosVM.loadAsync(context, tagsVM) }
            }
        }
    }

    LaunchedEffect(Unit) {
        videosVM.hasPermission.value = AppFeatureType.FILES.hasPermission(context)
        if (videosVM.hasPermission.value) {
            scope.launch(Dispatchers.IO) {
                cellsPerRow.value = VideoGridCellsPerRowPreference.getAsync()
                videosVM.sortBy.value = VideoSortByPreference.getValueAsync()
                videosVM.loadAsync(context, tagsVM)
                mediaFoldersVM.loadAsync(context)
            }
        }
    }
    LaunchedEffect(Channel.sharedFlow) {
        Channel.sharedFlow.collect { event ->
            when (event) {
                is PermissionsResultEvent -> {
                    videosVM.hasPermission.value = AppFeatureType.FILES.hasPermission(context)
                    scope.launch(Dispatchers.IO) { videosVM.sortBy.value = VideoSortByPreference.getValueAsync(); videosVM.loadAsync(context, tagsVM) }
                }
            }
        }
    }
    LaunchedEffect(dragSelectState.selectMode, (previewerState.visible && !context.isGestureInteractionMode())) {
        if (dragSelectState.selectMode || (previewerState.visible && !context.isGestureInteractionMode())) scrollBehavior.reset()
    }
    LaunchedEffect(pagerState.currentPage) {
        if (isFirstTime.value) {
            isFirstTime.value = false
            return@LaunchedEffect
        }
        val tab = tabs.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        when (tab.value) {
            "all" -> {
                videosVM.trash.value = false; videosVM.tag.value = null
            }
            "trash" -> {
                videosVM.trash.value = true; videosVM.tag.value = null
            }
            else -> {
                videosVM.trash.value = false; videosVM.tag.value = tagsState.find { it.id == tab.value }
            }
        }
        scope.launch {
            scrollBehavior.reset()
            videosVM.scrollStateMap[pagerState.currentPage]?.scrollToItem(0)
        }
        scope.launch(Dispatchers.IO) { videosVM.loadAsync(context, tagsVM) }
    }

    ViewVideoBottomSheet(videosVM, tagsVM, tagsMapState, tagsState, dragSelectState)
    MediaFoldersBottomSheet(videosVM, mediaFoldersVM, tagsVM)
    if (videosVM.showTagsDialog.value) {
        TagsBottomSheet(tagsVM) { videosVM.showTagsDialog.value = false }
    }
    CastDialog(castVM)

    PScaffold(
        topBar = {
            MediaTopBar(
                navController = navController,
                mediaVM = videosVM,
                tagsVM = tagsVM,
                castVM = castVM,
                dragSelectState = dragSelectState,
                scrollBehavior = scrollBehavior,
                bucketsMap = bucketsMap,
                itemsState = itemsState,
                scrollToTop = { scope.launch { videosVM.scrollStateMap[pagerState.currentPage]?.scrollToItem(0) } },
                defaultNavigationIcon = { NavigationBackIcon { navController.popBackStack() } },
                onSortSelected = { _, sortBy ->
                    scope.launch(Dispatchers.IO) {
                        VideoSortByPreference.putAsync(sortBy)
                        videosVM.sortBy.value = sortBy
                        videosVM.loadAsync(context, tagsVM)
                    }
                },
                onSearchAction = { ctx, tv -> scope.launch(Dispatchers.IO) { videosVM.loadAsync(ctx, tv) } },
            )
        },
        bottomBar = {
            AnimatedBottomAction(visible = dragSelectState.showBottomActions()) {
                MediaFilesSelectModeBottomActions(
                    vm = videosVM,
                    tagsVM = tagsVM,
                    tagsState = tagsState,
                    dragSelectState = dragSelectState,
                    getItemUri = { VideoMediaStoreHelper.getItemUri(it) },
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
                NeedPermissionColumn(Res.drawable.video, AppFeatureType.FILES.getPermission()!!); return@PScaffold
            }
            if (!dragSelectState.selectMode) {
                PScrollableTabRow(selectedTabIndex = pagerState.currentPage, modifier = Modifier.fillMaxWidth()) {
                    tabs.forEachIndexed { index, s ->
                        PFilterChip(
                            modifier = Modifier.padding(start = if (index == 0) 0.dp else 8.dp),
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.scrollToPage(index) } },
                            label = {
                                if (index == 0) Text(text = s.title + " (" + s.count + ")")
                                else Text(if (videosVM.bucketId.value.isNotEmpty() || videosVM.queryText.value.isNotEmpty()) s.title else "${s.title} (${s.count})")
                            },
                        )
                    }
                }
            }
            if (pagerState.pageCount == 0) {
                NoDataColumn(loading = videosVM.showLoading.value, search = videosVM.showSearchBar.value)
                return@Column
            }
            HorizontalPager(state = pagerState) { index ->
                PullToRefresh(refreshLayoutState = topRefreshLayoutState) {
                    AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                        if (itemsState.isNotEmpty()) {
                            val scrollState = rememberLazyGridState()
                            videosVM.scrollStateMap[index] = scrollState
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
                                            VideoGridCellsPerRowPreference.putAsync(it)
                                        },
                                    horizontalArrangement = Arrangement.spacedBy(2.dp),
                                    verticalArrangement = Arrangement.spacedBy(2.dp),
                                ) {
                                    val isGroupMode = videosVM.sortBy.value == FileSortBy.TAKEN_AT_DESC
                                            && videosVM.queryText.value.isEmpty()
                                    if (isGroupMode) {
                                        val groupedItems = groupMediaByDate(itemsState) { it.takenAt ?: it.createdAt }
                                        groupedItems.forEach { group ->
                                            item(span = { GridItemSpan(maxLineSpan) }, key = "header_${group.dateKey}", contentType = "header") {
                                                Text(text = group.dateLabel, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.titleSmall)
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
                                            LaunchedEffect(Unit) { scope.launch(Dispatchers.IO) { videosVM.moreAsync(context, tagsVM) } }
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
    }

    MediaPreviewer(
        state = previewerState,
        tagsVM = tagsVM,
        tagsMap = tagsMapState,
        tagsState = tagsState,
        onRenamed = { scope.launch(Dispatchers.IO) { videosVM.loadAsync(context, tagsVM) } },
        deleteAction = { item ->
            scope.launch(Dispatchers.IO) {
                videosVM.delete(context, tagsVM, setOf(item.mediaId))
                previewerState.closeTransform()
            }
        },
        onTagsChanged = {},
    )
}