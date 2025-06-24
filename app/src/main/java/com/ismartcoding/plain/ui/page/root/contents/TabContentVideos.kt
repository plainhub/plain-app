package com.ismartcoding.plain.ui.page.root.contents

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.extensions.isGestureInteractionMode
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.preferences.VideoGridCellsPerRowPreference
import com.ismartcoding.plain.preferences.VideoSortByPreference
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NeedPermissionColumn
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PScrollableTabRow
import com.ismartcoding.plain.ui.base.RadioDialog
import com.ismartcoding.plain.ui.base.RadioDialogOption
import com.ismartcoding.plain.ui.base.dragselect.gridDragSelect
import com.ismartcoding.plain.ui.base.fastscroll.LazyVerticalGridScrollbar
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.page.cast.CastDialog
import com.ismartcoding.plain.ui.components.FileSortDialog
import com.ismartcoding.plain.ui.components.VideoGridItem
import com.ismartcoding.plain.ui.extensions.reset
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VTabData
import com.ismartcoding.plain.ui.models.VideosViewModel
import com.ismartcoding.plain.ui.page.root.MediaFoldersBottomSheet
import com.ismartcoding.plain.ui.page.tags.TagsBottomSheet
import com.ismartcoding.plain.ui.page.videos.VideosPageState
import com.ismartcoding.plain.ui.page.videos.ViewVideoBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TabContentVideos(
    videosState: VideosPageState,
    videosVM: VideosViewModel,
    tagsVM: TagsViewModel,
    mediaFoldersVM: MediaFoldersViewModel,
    castVM: CastViewModel,
    paddingValues: PaddingValues
) {
    val context = LocalContext.current
    val itemsState = videosState.itemsState
    val configuration = LocalConfiguration.current
    val pagerState = videosState.pagerState
    val scrollBehavior = videosState.scrollBehavior
    val tagsState = videosState.tagsState
    val previewerState = videosState.previewerState
    val tagsMapState = videosState.tagsMapState
    val dragSelectState = videosState.dragSelectState
    val cellsPerRow = videosState.cellsPerRow

    val scope = rememberCoroutineScope()
    var isFirstTime by remember { mutableStateOf(true) }
    val density = LocalDensity.current
    val imageWidthPx = remember(cellsPerRow.value) {
        density.run { ((configuration.screenWidthDp.dp - ((cellsPerRow.value - 1) * 2).dp) / cellsPerRow.value).toPx().toInt() }
    }
    val sharedFlow = Channel.sharedFlow
    val tabs = remember(tagsState, videosVM.total.intValue, videosVM.totalTrash.intValue) {
        val baseTabs = mutableListOf(
            VTabData(LocaleHelper.getString(R.string.all), "all", videosVM.total.intValue)
        )
        if (AppFeatureType.MEDIA_TRASH.has()) {
            baseTabs.add(VTabData(LocaleHelper.getString(R.string.trash), "trash", videosVM.totalTrash.intValue))
        }
        baseTabs.addAll(tagsState.map { VTabData(it.name, it.id, it.count) })
        baseTabs
    }

    val topRefreshLayoutState =
        rememberRefreshLayoutState {
            scope.launch {
                withIO {
                    videosVM.loadAsync(context, tagsVM)
                    mediaFoldersVM.loadAsync(context)
                }
                setRefreshState(RefreshContentState.Finished)
            }
        }

    LaunchedEffect(Unit) {
        videosVM.hasPermission.value = AppFeatureType.FILES.hasPermission(context)
        if (videosVM.hasPermission.value) {
            scope.launch(Dispatchers.IO) {
                videosVM.sortBy.value = VideoSortByPreference.getValueAsync(context)
                videosVM.loadAsync(context, tagsVM)
                mediaFoldersVM.loadAsync(context)
            }
        }
    }

    LaunchedEffect(sharedFlow) {
        sharedFlow.collect { event ->
            when (event) {
                is PermissionsResultEvent -> {
                    videosVM.hasPermission.value = AppFeatureType.FILES.hasPermission(context)
                    scope.launch(Dispatchers.IO) {
                        videosVM.sortBy.value = VideoSortByPreference.getValueAsync(context)
                        videosVM.loadAsync(context, tagsVM)
                    }
                }
            }
        }
    }

    LaunchedEffect(dragSelectState.selectMode, (previewerState.visible && !context.isGestureInteractionMode())) {
        if (dragSelectState.selectMode || (previewerState.visible && !context.isGestureInteractionMode())) {
            scrollBehavior.reset()
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (isFirstTime) {
            isFirstTime = false
            return@LaunchedEffect
        }
        val tab = tabs.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        when (tab.value) {
            "all" -> {
                videosVM.trash.value = false
                videosVM.tag.value = null
            }
            "trash" -> {
                videosVM.trash.value = true
                videosVM.tag.value = null
            }
            else -> {
                val tag = tagsState.find { it.id == tab.value }
                videosVM.trash.value = false
                videosVM.tag.value = tag
            }
        }
        scope.launch {
            scrollBehavior.reset()
            videosVM.scrollStateMap[pagerState.currentPage]?.scrollToItem(0)
        }
        scope.launch(Dispatchers.IO) {
            videosVM.loadAsync(context, tagsVM)
        }
    }

    if (videosVM.showCellsPerRowDialog.value) {
        RadioDialog(
            title = stringResource(R.string.cells_per_row),
            options = IntRange(2, 10).map { value ->
                RadioDialogOption(
                    text = value.toString(),
                    selected = value == cellsPerRow.value,
                ) {
                    scope.launch(Dispatchers.IO) {
                        VideoGridCellsPerRowPreference.putAsync(context, value)
                        cellsPerRow.value = value
                    }
                }
            },
        ) {
            videosVM.showCellsPerRowDialog.value = false
        }
    }

    ViewVideoBottomSheet(videosVM, tagsVM, tagsMapState, tagsState, dragSelectState)

    if (videosVM.showSortDialog.value) {
        FileSortDialog(videosVM.sortBy, onSelected = {
            scope.launch(Dispatchers.IO) {
                VideoSortByPreference.putAsync(context, it)
                videosVM.sortBy.value = it
                videosVM.loadAsync(context, tagsVM)
            }
        }, onDismiss = {
            videosVM.showSortDialog.value = false
        })
    }

    MediaFoldersBottomSheet(videosVM, mediaFoldersVM, tagsVM)
    if (videosVM.showTagsDialog.value) {
        TagsBottomSheet(tagsVM) {
            videosVM.showTagsDialog.value = false
        }
    }
    CastDialog(castVM)
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(top = paddingValues.calculateTopPadding())
    ) {

        if (!videosVM.hasPermission.value) {
            NeedPermissionColumn(R.drawable.video, AppFeatureType.FILES.getPermission()!!)
            return
        }

        if (!dragSelectState.selectMode) {
            PScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                tabs.forEachIndexed { index, s ->
                    PFilterChip(
                        modifier = Modifier.padding(start = if (index == 0) 0.dp else 8.dp),
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.scrollToPage(index)
                            }
                        },
                        label = {
                            if (index == 0) {
                                Text(text = s.title + " (" + s.count + ")")
                            } else {
                                Text(if (videosVM.bucketId.value.isNotEmpty() || videosVM.queryText.value.isNotEmpty()) s.title else "${s.title} (${s.count})")
                            }
                        }
                    )
                }
            }
        }
        if (pagerState.pageCount == 0) {
            NoDataColumn(loading = videosVM.showLoading.value, search = videosVM.showSearchBar.value)
            return
        }
        HorizontalPager(state = pagerState) { index ->
            PullToRefresh(
                refreshLayoutState = topRefreshLayoutState,
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (itemsState.isNotEmpty()) {
                        val scrollState = rememberLazyGridState()
                        videosVM.scrollStateMap[index] = scrollState
                        LazyVerticalGridScrollbar(
                            state = scrollState,
                        ) {
                            LazyVerticalGrid(
                                columns = GridCells.Fixed(cellsPerRow.value),
                                state = scrollState,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .nestedScroll(scrollBehavior.nestedScrollConnection)
                                    .gridDragSelect(
                                        items = itemsState,
                                        state = dragSelectState,
                                    ),
                                horizontalArrangement = Arrangement.spacedBy(2.dp),
                                verticalArrangement = Arrangement.spacedBy(2.dp),
                            ) {
                                items(
                                    itemsState,
                                    key = {
                                        it.id
                                    },
                                    contentType = {
                                        "video"
                                    },
                                    span = {
                                        GridItemSpan(1)
                                    }) { m ->
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
                                item(
                                    span = { GridItemSpan(maxLineSpan) },
                                    key = "loadMore"
                                ) {
                                    if (itemsState.isNotEmpty() && !videosVM.noMore.value) {
                                        LaunchedEffect(Unit) {
                                            scope.launch(Dispatchers.IO) {
                                                withIO { videosVM.moreAsync(context, tagsVM) }
                                            }
                                        }
                                    }
                                    LoadMoreRefreshContent(videosVM.noMore.value)
                                }
                                item(
                                    span = { GridItemSpan(maxLineSpan) },
                                    key = "bottomSpace"
                                ) {
                                    BottomSpace(paddingValues)
                                }
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