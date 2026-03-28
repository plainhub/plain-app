package com.ismartcoding.plain.ui.page.apps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.base.fastscroll.LazyColumnScrollbar
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshLayoutState
import com.ismartcoding.plain.ui.components.PackageListItem
import com.ismartcoding.plain.ui.models.AppsViewModel
import com.ismartcoding.plain.ui.models.VPackage
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.theme.PlainTheme
import androidx.compose.foundation.layout.PaddingValues
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsPageList(
    navController: NavHostController,
    appsVM: AppsViewModel,
    items: List<VPackage>,
    index: Int,
    scrollStateMap: MutableMap<Int, LazyListState>,
    scrollBehavior: TopAppBarScrollBehavior,
    topRefreshLayoutState: RefreshLayoutState,
    paddingValues: PaddingValues,
) {
    val scope = rememberCoroutineScope()
    PullToRefresh(refreshLayoutState = topRefreshLayoutState) {
        AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
            if (items.isNotEmpty()) {
                val scrollState = rememberLazyListState()
                scrollStateMap[index] = scrollState
                LazyColumnScrollbar(state = scrollState) {
                    LazyColumn(
                        Modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
                        state = scrollState,
                    ) {
                        item { TopSpace() }
                        items(items, key = { it.id }) { m ->
                            PackageListItem(item = m, modifier = PlainTheme.getCardModifier(), onClick = { navController.navigate(Routing.AppDetails(m.id)) })
                            VerticalSpace(dp = 8.dp)
                        }
                        item {
                            if (items.isNotEmpty() && !appsVM.noMore.value) {
                                LaunchedEffect(Unit) { scope.launch(Dispatchers.IO) { withIO { appsVM.moreAsync() } } }
                            }
                            LoadMoreRefreshContent(appsVM.noMore.value)
                            BottomSpace(paddingValues)
                        }
                    }
                }
            } else {
                NoDataColumn(loading = appsVM.showLoading.value, search = appsVM.showSearchBar.value)
            }
        }
    }
}
