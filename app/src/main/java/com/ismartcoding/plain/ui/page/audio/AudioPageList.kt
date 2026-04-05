package com.ismartcoding.plain.ui.page.audio

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.data.DAudio
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.base.dragselect.listDragSelect
import com.ismartcoding.plain.ui.base.fastscroll.LazyColumnScrollbar
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshLayoutState
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.page.audio.components.AudioListItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun ColumnScope.AudioPageList(
    pagerState: PagerState, scrollBehavior: TopAppBarScrollBehavior,
    dragSelectState: DragSelectState, itemsState: List<DAudio>,
    audioVM: AudioViewModel, audioPlaylistVM: AudioPlaylistViewModel,
    tagsVM: TagsViewModel, castVM: CastViewModel,
    audioTagsMap: Map<String, List<DTag>>, isAudioPlaying: Boolean,
    topRefreshLayoutState: RefreshLayoutState, paddingValues: PaddingValues,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    HorizontalPager(state = pagerState, modifier = Modifier.weight(1f)) { index ->
        PullToRefresh(refreshLayoutState = topRefreshLayoutState, userEnable = !dragSelectState.selectMode) {
            AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                if (itemsState.isNotEmpty()) {
                    val scrollState = rememberLazyListState()
                    audioVM.scrollStateMap[index] = scrollState
                    LazyColumnScrollbar(state = scrollState) {
                        LazyColumn(Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection)
                            .listDragSelect(items = itemsState, state = dragSelectState), state = scrollState) {
                            item { TopSpace() }
                            items(items = itemsState, key = { it.id }) { item ->
                                val tags = audioTagsMap[item.id] ?: emptyList()
                                AudioListItem(item = item, audioVM = audioVM, audioPlaylistVM, tagsVM = tagsVM,
                                    castVM = castVM, tags = tags, pagerState = pagerState, dragSelectState = dragSelectState,
                                    isCurrentlyPlaying = isAudioPlaying && audioPlaylistVM.selectedPath.value == item.path,
                                    isInPlaylist = audioPlaylistVM.isInPlaylist(item.path))
                                VerticalSpace(dp = 8.dp)
                            }
                            item(key = "loadMore") {
                                if (itemsState.isNotEmpty() && !audioVM.noMore.value) {
                                    LaunchedEffect(Unit) {
                                        scope.launch(Dispatchers.IO) { withIO { audioVM.moreAsync(context, tagsVM) } }
                                    }
                                }
                                LoadMoreRefreshContent(audioVM.noMore.value)
                            }
                            item(key = "bottomSpace") { BottomSpace(paddingValues) }
                        }
                    }
                } else {
                    NoDataColumn(loading = audioVM.showLoading.value, search = audioVM.showSearchBar.value)
                }
            }
        }
    }
}
