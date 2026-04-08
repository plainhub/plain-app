package com.ismartcoding.plain.ui.page.videos

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.data.DVideo
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.preferences.VideoGridCellsPerRowPreference
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.base.dragselect.gridDragSelect
import com.ismartcoding.plain.ui.base.fastscroll.LazyVerticalGridScrollbar
import com.ismartcoding.plain.ui.base.pinchZoomGrid
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshLayoutState
import com.ismartcoding.plain.ui.base.rememberBoostFlingBehavior
import com.ismartcoding.plain.ui.components.VideoGridItem
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.helpers.groupMediaByDate
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VideosViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VideosPageGrid(
    context: Context, videosVM: VideosViewModel, tagsVM: TagsViewModel, castVM: CastViewModel,
    itemsState: List<DVideo>, pagerState: PagerState, scrollBehavior: TopAppBarScrollBehavior,
    topRefreshLayoutState: RefreshLayoutState, dragSelectState: DragSelectState,
    previewerState: MediaPreviewerState, cellsPerRow: MutableState<Int>,
    imageWidthPx: Int, scope: CoroutineScope, paddingValues: PaddingValues,
) {
    val hapticFeedback = LocalHapticFeedback.current
    if (pagerState.pageCount == 0) { NoDataColumn(loading = videosVM.showLoading.value, search = videosVM.showSearchBar.value); return }
    HorizontalPager(state = pagerState) { index ->
        PullToRefresh(refreshLayoutState = topRefreshLayoutState) {
            AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                if (itemsState.isNotEmpty()) {
                    val scrollState = rememberLazyGridState()
                    videosVM.scrollStateMap[index] = scrollState
                    val flingBehavior = rememberBoostFlingBehavior(cellsPerRow.value / 3f)
                    LazyVerticalGridScrollbar(state = scrollState) {
                        LazyVerticalGrid(columns = GridCells.Fixed(cellsPerRow.value), state = scrollState, flingBehavior = flingBehavior,
                            modifier = Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)
                                .gridDragSelect(items = itemsState, state = dragSelectState)
                                .pinchZoomGrid(cellsPerRow = cellsPerRow, hapticFeedback = hapticFeedback, scope = scope) { VideoGridCellsPerRowPreference.putAsync(context, it) },
                            horizontalArrangement = Arrangement.spacedBy(2.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            val isGroupMode = videosVM.sortBy.value == FileSortBy.TAKEN_AT_DESC
                                && videosVM.queryText.value.isEmpty()
                            if (isGroupMode) {
                                val groupedItems = groupMediaByDate(itemsState) { it.takenAt ?: it.createdAt }
                                groupedItems.forEach { group ->
                                    item(span = { GridItemSpan(maxLineSpan) }, key = "header_${group.dateKey}", contentType = "header") {
                                        Text(text = group.dateLabel, modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp), style = MaterialTheme.typography.titleSmall)
                                    }
                                    items(group.items, key = { it.id }, contentType = { "video" }, span = { GridItemSpan(1) }) { m ->
                                        VideoGridItem(modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null), videosVM, castVM, m, showSize = cellsPerRow.value < 6, previewerState, dragSelectState, imageWidthPx, sort = videosVM.sortBy.value)
                                    }
                                }
                            } else {
                                items(itemsState, key = { it.id }, contentType = { "video" }, span = { GridItemSpan(1) }) { m ->
                                    VideoGridItem(modifier = Modifier.animateItem(fadeInSpec = null, fadeOutSpec = null), videosVM, castVM, m, showSize = cellsPerRow.value < 6, previewerState, dragSelectState, imageWidthPx, sort = videosVM.sortBy.value)
                                }
                            }
                            item(span = { GridItemSpan(maxLineSpan) }, key = "loadMore") {
                                if (itemsState.isNotEmpty() && !videosVM.noMore.value) { LaunchedEffect(Unit) { scope.launch(Dispatchers.IO) { withIO { videosVM.moreAsync(context, tagsVM) } } } }
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
