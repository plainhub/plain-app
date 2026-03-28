package com.ismartcoding.plain.ui.page.docs

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
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
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.fastscroll.LazyColumnScrollbar
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshLayoutState
import com.ismartcoding.plain.ui.components.DocItem
import com.ismartcoding.plain.ui.models.DocsViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DocsPageContent(
    navController: NavHostController, docsVM: DocsViewModel,
    filteredItemsState: List<DFile>, scrollStateMap: MutableMap<Int, LazyListState>,
    pagerState: PagerState, scrollBehavior: TopAppBarScrollBehavior,
    topRefreshLayoutState: RefreshLayoutState, scope: CoroutineScope,
    context: android.content.Context, bottomPadding: Dp,
) {
    if (pagerState.pageCount == 0) { NoDataColumn(loading = docsVM.showLoading.value, search = docsVM.showSearchBar.value); return }
    HorizontalPager(state = pagerState, userScrollEnabled = false) { index ->
        PullToRefresh(refreshLayoutState = topRefreshLayoutState) {
            AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                if (filteredItemsState.isNotEmpty()) {
                    val scrollState = rememberLazyListState()
                    scrollStateMap[index] = scrollState
                    LazyColumnScrollbar(state = scrollState) {
                        LazyColumn(Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection), state = scrollState) {
                            item { TopSpace() }
                            items(filteredItemsState) { m -> DocItem(navController, docsVM, m); VerticalSpace(dp = 8.dp) }
                            item {
                                if (filteredItemsState.isNotEmpty() && !docsVM.noMore.value) { LaunchedEffect(Unit) { scope.launch(Dispatchers.IO) { withIO { docsVM.moreAsync(context) } } } }
                                LoadMoreRefreshContent(docsVM.noMore.value)
                            }
                            item { VerticalSpace(dp = bottomPadding) }
                        }
                    }
                } else {
                    NoDataColumn(loading = docsVM.showLoading.value, search = docsVM.showSearchBar.value)
                }
            }
        }
    }
}
