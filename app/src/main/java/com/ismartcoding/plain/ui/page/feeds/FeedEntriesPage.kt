package com.ismartcoding.plain.ui.page.feeds

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.FeedEntryFilterType
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.ActionButtonMoreWithMenu
import com.ismartcoding.plain.ui.base.ActionButtonSearch
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.PDropdownMenuItemSettings
import com.ismartcoding.plain.ui.base.PDropdownMenuItemTags
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.PTopRightButton
import com.ismartcoding.plain.ui.components.ListSearchBar
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.models.FeedEntriesViewModel
import com.ismartcoding.plain.ui.models.FeedsViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VTabData
import com.ismartcoding.plain.ui.models.enterSearchMode
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.isAllSelected
import com.ismartcoding.plain.ui.models.showBottomActions
import com.ismartcoding.plain.ui.models.toggleSelectAll
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.tags.TagsBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FeedEntriesPage(
    navController: NavHostController, feedId: String, tagsVM: TagsViewModel,
    feedEntriesVM: FeedEntriesViewModel = viewModel(), feedsVM: FeedsViewModel = viewModel(),
) {
    val feedsState by feedsVM.itemsFlow.collectAsState()
    val feedsMap = remember(feedsState) { derivedStateOf { feedsState.associateBy { it.id } } }
    val tagsState by tagsVM.itemsFlow.collectAsState()
    val tagsMapState by tagsVM.tagsMapFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollStateMap = remember { mutableStateMapOf<Int, LazyListState>() }
    val pagerState = rememberPagerState(pageCount = { tagsState.size + 2 })
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = {
        (scrollStateMap[pagerState.currentPage]?.firstVisibleItemIndex ?: 0) > 0 && !feedEntriesVM.selectMode.value
    })
    val isFirstTime = remember { mutableStateOf(true) }
    val tabs = remember(tagsState, feedEntriesVM.total.intValue) {
        listOf(VTabData(LocaleHelper.getString(R.string.all), "all", feedEntriesVM.total.intValue),
            VTabData(LocaleHelper.getString(R.string.today), "today", feedEntriesVM.totalToday.value),
            *tagsState.map { VTabData(it.name, it.id, it.count) }.toTypedArray())
    }
    val topRefreshLayoutState = rememberRefreshLayoutState { scope.launch { feedEntriesVM.sync() } }

    val onSearch: (String) -> Unit = {
        feedEntriesVM.searchActive.value = false; feedEntriesVM.showLoading.value = true
        scope.launch { scrollStateMap[pagerState.currentPage]?.scrollToItem(0) }
        scope.launch(Dispatchers.IO) { feedEntriesVM.loadAsync(tagsVM) }
    }

    FeedEntriesPageEffects(feedEntriesVM, feedsVM, tagsVM, feedId, scope, tabs, pagerState, scrollBehavior, scrollStateMap, topRefreshLayoutState, isFirstTime, onSearch)

    val feed = if (feedEntriesVM.feedId.value.isEmpty()) null else feedsMap.value[feedEntriesVM.feedId.value]
    val feedName = feed?.name ?: stringResource(id = R.string.feeds)
    val pageTitle = if (feedEntriesVM.selectMode.value) LocaleHelper.getStringF(R.string.x_selected, "count", feedEntriesVM.selectedIds.size)
        else if (feedEntriesVM.tag.value != null) listOf(feedName, feedEntriesVM.tag.value!!.name).joinToString(" - ")
        else if (feedEntriesVM.filterType == FeedEntryFilterType.TODAY) feedName + " - " + stringResource(id = R.string.today) else feedName

    ViewFeedEntryBottomSheet(feedEntriesVM, tagsVM, tagsMapState, tagsState)
    if (feedEntriesVM.showTagsDialog.value) { TagsBottomSheet(tagsVM) { feedEntriesVM.showTagsDialog.value = false } }

    PScaffold(topBar = {
        if (feedEntriesVM.showSearchBar.value) { ListSearchBar(viewModel = feedEntriesVM, onSearch = onSearch); return@PScaffold }
        PTopAppBar(modifier = Modifier.combinedClickable(onClick = {}, onDoubleClick = { scope.launch { scrollStateMap[pagerState.currentPage]?.scrollToItem(0) } }),
            navController = navController, navigationIcon = {
                if (feedEntriesVM.selectMode.value) NavigationCloseIcon { feedEntriesVM.exitSelectMode() } else NavigationBackIcon { navController.navigateUp() }
            }, title = pageTitle, scrollBehavior = scrollBehavior, actions = {
                if (feedEntriesVM.selectMode.value) { PTopRightButton(label = stringResource(if (feedEntriesVM.isAllSelected()) R.string.unselect_all else R.string.select_all), click = { feedEntriesVM.toggleSelectAll() }); HorizontalSpace(dp = 8.dp) }
                else {
                    ActionButtonSearch { feedEntriesVM.enterSearchMode() }
                    if (feedEntriesVM.feedId.value.isEmpty()) { PIconButton(icon = R.drawable.rss, contentDescription = stringResource(R.string.subscriptions), tint = MaterialTheme.colorScheme.onSurface) { navController.navigate(Routing.Feeds) } }
                    ActionButtonMoreWithMenu { dismiss -> PDropdownMenuItemTags(onClick = { dismiss(); feedEntriesVM.showTagsDialog.value = true })
                        if (feedEntriesVM.feedId.value.isEmpty()) PDropdownMenuItemSettings(onClick = { dismiss(); navController.navigate(Routing.FeedSettings) }) }
                }
            })
    }, bottomBar = {
        AnimatedVisibility(visible = feedEntriesVM.showBottomActions(), enter = slideInVertically { it }, exit = slideOutVertically { it }) {
            FeedEntriesSelectModeBottomActions(feedEntriesVM, tagsVM, tagsState)
        }
    }) { paddingValues ->
        Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
            FeedEntriesPageContent(feedEntriesVM, tagsVM, navController, scope, tabs, pagerState, scrollBehavior, scrollStateMap,
                topRefreshLayoutState, feedsMap, tagsState, tagsMapState, paddingValues.calculateBottomPadding().value)
        }
    }
}
