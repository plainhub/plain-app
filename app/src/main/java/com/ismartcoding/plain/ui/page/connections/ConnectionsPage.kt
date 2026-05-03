package com.ismartcoding.plain.ui.page.connections

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.ButtonSize
import com.ismartcoding.plain.ui.base.ActionButtonAdd
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PScrollableTabRow
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.models.SessionsViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ConnectionsPage(
    navController: NavHostController,
    sessionsVM: SessionsViewModel = viewModel(),
) {
    val itemsState by sessionsVM.itemsFlow.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(pageCount = { 2 })
    val sessions = remember(itemsState) { itemsState.filter { !it.isCustom } }
    val apiTokens = remember(itemsState) { itemsState.filter { it.isCustom } }
    val tabTitles = listOf(stringResource(R.string.sessions), stringResource(R.string.api_tokens))

    val refreshState = rememberRefreshLayoutState {
        sessionsVM.fetch()
        setRefreshState(RefreshContentState.Finished)
    }

    LaunchedEffect(Unit) {
        sessionsVM.fetch()
    }

    if (showCreateDialog) {
        CreateApiTokenDialog(
            onDismiss = { showCreateDialog = false },
            onCreate = { name ->
                showCreateDialog = false
                sessionsVM.createCustomToken(name)
            },
        )
    }

    PScaffold(
        topBar = {
            Column {
                TopAppBar(
                    navigationIcon = { NavigationBackIcon { navController.navigateUp() } },
                    title = {
                        PScrollableTabRow(selectedTabIndex = pagerState.currentPage) {
                            tabTitles.forEachIndexed { index, title ->
                                PFilterChip(
                                    modifier = Modifier.padding(start = if (index == 0) 0.dp else 8.dp),
                                    selected = pagerState.currentPage == index,
                                    onClick = { scope.launch { pagerState.scrollToPage(index) } },
                                    label = { Text(text = title) },
                                )
                            }
                        }
                    },
                    actions = {
                        if (pagerState.currentPage == 1) {
                            ActionButtonAdd {
                                showCreateDialog = true
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    ),
                )
            }
        },
        content = { paddingValues ->
            HorizontalPager(state = pagerState, userScrollEnabled = true) { page ->
                PullToRefresh(refreshLayoutState = refreshState) {
                    val items = if (page == 0) sessions else apiTokens
                    if (items.isNotEmpty()) {
                        LazyColumn(
                            Modifier
                                .fillMaxWidth()
                                .fillMaxHeight()
                                .padding(top = paddingValues.calculateTopPadding())
                        ) {
                            item { TopSpace() }
                            items(items) { m ->
                                SessionListItem(
                                    m = m,
                                    onDelete = { sessionsVM.delete(it) },
                                    onRename = { clientId, name -> sessionsVM.rename(clientId, name) },
                                )
                            }
                            item { BottomSpace(paddingValues) }
                        }
                    } else {
                        NoDataColumn()
                    }
                }
            }
        },
    )
}

