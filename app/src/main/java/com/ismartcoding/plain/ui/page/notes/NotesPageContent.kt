package com.ismartcoding.plain.ui.page.notes

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.db.DNote
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.fastscroll.LazyColumnScrollbar
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshLayoutState
import com.ismartcoding.plain.ui.components.NoteListItem
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VTabData
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.nav.Routing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun NotesPageContent(
    notesVM: NotesViewModel, tagsVM: TagsViewModel,
    itemsState: List<DNote>, tagsState: List<DTag>, tagsMapState: Map<String, List<DTagRelation>>,
    scrollStateMap: MutableMap<Int, LazyListState>, index: Int,
    scrollBehavior: TopAppBarScrollBehavior, topRefreshLayoutState: RefreshLayoutState,
    navController: NavHostController, bottomPadding: Dp,
    tabs: List<VTabData>, scope: CoroutineScope, pagerState: PagerState,
) {
    PullToRefresh(refreshLayoutState = topRefreshLayoutState) {
        AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
            if (itemsState.isNotEmpty()) {
                val scrollState = rememberLazyListState()
                scrollStateMap[index] = scrollState
                LazyColumnScrollbar(state = scrollState) {
                    LazyColumn(Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection), state = scrollState) {
                        item { TopSpace() }
                        items(itemsState, key = { it.id }) { m ->
                            val tagIds = tagsMapState[m.id]?.map { it.tagId } ?: emptyList()
                            NoteListItem(notesVM, m, tagsState.filter { tagIds.contains(it.id) },
                                onClick = { if (notesVM.selectMode.value) notesVM.select(m.id) else navController.navigate(Routing.NoteDetail(m.id)) },
                                onLongClick = { if (notesVM.selectMode.value) return@NoteListItem; notesVM.selectedItem.value = m },
                                onClickTag = { tag ->
                                    if (notesVM.selectMode.value) return@NoteListItem
                                    val idx = tabs.indexOfFirst { it.value == tag.id }
                                    if (idx != -1) scope.launch { pagerState.scrollToPage(idx) }
                                },
                            )
                            VerticalSpace(dp = 8.dp)
                        }
                        item {
                            if (itemsState.isNotEmpty() && !notesVM.noMore.value) {
                                LaunchedEffect(Unit) { scope.launch(Dispatchers.IO) { withIO { notesVM.moreAsync(tagsVM) } } }
                            }
                            LoadMoreRefreshContent(notesVM.noMore.value)
                        }
                        item { VerticalSpace(dp = bottomPadding) }
                    }
                }
            } else {
                NoDataColumn(loading = notesVM.showLoading.value, search = notesVM.showSearchBar.value)
            }
        }
    }
}
