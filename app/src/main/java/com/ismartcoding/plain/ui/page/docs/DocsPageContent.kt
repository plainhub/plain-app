package com.ismartcoding.plain.ui.page.docs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.docs.DDoc
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
import com.ismartcoding.plain.ui.components.DocItem
import com.ismartcoding.plain.ui.models.DocsViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ColumnScope.DocsPageContent(
    navController: NavHostController, docsVM: DocsViewModel,
    tagsVM: TagsViewModel,
    itemsState: List<DDoc>,
    dragSelectState: DragSelectState,
    docsTagsMap: Map<String, List<DTag>>,
    pagerState: PagerState, scrollBehavior: TopAppBarScrollBehavior,
    topRefreshLayoutState: RefreshLayoutState, paddingValues: PaddingValues,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    if (pagerState.pageCount == 0) {
        NoDataColumn(loading = docsVM.showLoading.value, search = docsVM.showSearchBar.value)
        return
    }

    HorizontalPager(
        state = pagerState,
        modifier = Modifier.weight(1f),
        userScrollEnabled = !dragSelectState.selectMode,
    ) { index ->
        PullToRefresh(refreshLayoutState = topRefreshLayoutState, userEnable = !dragSelectState.selectMode) {
            AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                if (itemsState.isNotEmpty()) {
                    val scrollState = rememberLazyListState()
                    docsVM.scrollStateMap[index] = scrollState
                    LazyColumnScrollbar(state = scrollState) {
                        LazyColumn(
                            Modifier
                                .fillMaxSize()
                                .nestedScroll(scrollBehavior.nestedScrollConnection)
                                .listDragSelect(items = itemsState, state = dragSelectState),
                            state = scrollState
                        ) {
                            item { TopSpace() }
                            items(itemsState, key = { it.id }) { m ->
                                val tags = docsTagsMap[m.id] ?: emptyList()
                                DocItem(
                                    navController = navController,
                                    docsVM = docsVM,
                                    dragSelectState = dragSelectState,
                                    m = m,
                                    tags = tags,
                                    onTagClick = { tag ->
                                        if (!docsVM.tabsShowTags.value) {
                                            return@DocItem
                                        }
                                        val idx = tagsVM.itemsFlow.value.indexOfFirst { it.id == tag.id }
                                        if (idx != -1) {
                                            scope.launch { pagerState.scrollToPage(idx + 1) }
                                        }
                                    }
                                )
                                VerticalSpace(dp = 8.dp)
                            }
                            item(key = "loadMore") {
                                if (itemsState.isNotEmpty() && !docsVM.noMore.value) {
                                    LaunchedEffect(Unit) {
                                        scope.launch(Dispatchers.IO) { withIO { docsVM.moreAsync(context, tagsVM) } }
                                    }
                                }
                                LoadMoreRefreshContent(docsVM.noMore.value)
                            }
                            item(key = "bottomSpace") { BottomSpace(paddingValues) }
                        }
                    }
                } else {
                    NoDataColumn(loading = docsVM.showLoading.value, search = docsVM.showSearchBar.value)
                }
            }
        }
    }
}
