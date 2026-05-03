package com.ismartcoding.plain.ui.page.feeds

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.ExportFileType
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.events.ExportFileEvent
import com.ismartcoding.plain.events.PickFileEvent
import com.ismartcoding.plain.extensions.formatName
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.FeedListItem
import com.ismartcoding.plain.ui.models.FeedsViewModel
import com.ismartcoding.plain.ui.models.showAddDialog
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.isAllSelected
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.models.showBottomActions
import com.ismartcoding.plain.ui.models.toggleSelectAll
import com.ismartcoding.plain.ui.nav.Routing
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FeedsPage(navController: NavHostController, feedsVM: FeedsViewModel = viewModel()) {
    val itemsState by feedsVM.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch { withIO { feedsVM.loadAsync(withCount = true) }; setRefreshState(RefreshContentState.Finished) }
    }
    LaunchedEffect(Unit) { scope.launch(Dispatchers.IO) { feedsVM.loadAsync(withCount = true) } }
    FeedsPageEffects(feedsVM)
    BackHandler(enabled = feedsVM.selectMode.value) { feedsVM.exitSelectMode() }
    AddFeedDialog(feedsVM); EditFeedDialog(feedsVM); ViewFeedBottomSheet(feedsVM)
    val pageTitle = if (feedsVM.selectMode.value) LocaleHelper.getStringF(R.string.x_selected, "count", feedsVM.selectedIds.size)
    else LocaleHelper.getStringF(R.string.subscriptions_title, "count", itemsState.size)

    PScaffold(
        topBar = {
            PTopAppBar(navController = navController, navigationIcon = {
                if (feedsVM.selectMode.value) NavigationCloseIcon { feedsVM.exitSelectMode() } else NavigationBackIcon { navController.navigateUp() }
            }, title = pageTitle, actions = {
                if (feedsVM.selectMode.value) {
                    PTopRightButton(label = stringResource(if (feedsVM.isAllSelected()) R.string.unselect_all else R.string.select_all), click = { feedsVM.toggleSelectAll() })
                    HorizontalSpace(dp = 8.dp)
                } else {
                    ActionButtonMoreWithMenu { dismiss ->
                        PDropdownMenuItem(
                            text = { Text(stringResource(R.string.import_opml_file)) },
                            leadingIcon = { Icon(painter = painterResource(R.drawable.upload), contentDescription = stringResource(R.string.import_opml_file)) },
                            onClick = { dismiss(); sendEvent(PickFileEvent(PickFileTag.FEED, PickFileType.FILE, false)) })
                        PDropdownMenuItem(
                            text = { Text(stringResource(R.string.export_opml_file)) },
                            leadingIcon = { Icon(painter = painterResource(R.drawable.download), contentDescription = stringResource(R.string.export_opml_file)) },
                            onClick = { dismiss(); sendEvent(ExportFileEvent(ExportFileType.OPML, "feeds_" + Date().formatName() + ".opml")) })
                    }
                }
            })
        },
        bottomBar = {
            AnimatedVisibility(visible = feedsVM.showBottomActions(), enter = slideInVertically { it }, exit = slideOutVertically { it }) { FeedsSelectModeBottomActions(feedsVM) }
        },
        floatingActionButton = if (feedsVM.selectMode.value) null else {
            {
                PDraggableElement {
                    FloatingActionButton(onClick = { feedsVM.showAddDialog() }) {
                        Icon(painter = painterResource(R.drawable.plus), stringResource(R.string.add))
                    }
                }
            }
        },
    ) { paddingValues ->
        PullToRefresh(modifier = Modifier.padding(top = paddingValues.calculateTopPadding()), refreshLayoutState = topRefreshLayoutState) {
            AnimatedVisibility(visible = true, enter = fadeIn(), exit = fadeOut()) {
                if (itemsState.isNotEmpty()) {
                    LazyColumn(Modifier.fillMaxSize()) {
                        item { TopSpace() }
                        items(itemsState) { m ->
                            FeedListItem(feedsVM = feedsVM, m, onClick = {
                                if (feedsVM.selectMode.value) feedsVM.select(m.id) else navController.navigate(Routing.FeedEntries(m.id))
                            }, onLongClick = { if (!feedsVM.selectMode.value) feedsVM.selectedItem.value = m })
                            VerticalSpace(dp = 8.dp)
                        }
                        item { VerticalSpace(dp = paddingValues.calculateBottomPadding()) }
                    }
                } else {
                    NoDataColumn(loading = feedsVM.showLoading.value)
                }
            }
        }
    }
}
