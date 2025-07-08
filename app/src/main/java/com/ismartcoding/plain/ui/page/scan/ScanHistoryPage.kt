package com.ismartcoding.plain.ui.page.scan

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.ScanHistoryViewModel
import com.ismartcoding.plain.ui.page.scan.components.ScanHistoryItem

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScanHistoryPage(
    navController: NavHostController,
    scanHistoryVM: ScanHistoryViewModel = viewModel(),
) {
    val context = LocalContext.current
    val itemsState by scanHistoryVM.itemsFlow.collectAsState()
    val refreshState =
        rememberRefreshLayoutState {
            scanHistoryVM.fetch(context)
            setRefreshState(RefreshContentState.Finished)
        }

    LaunchedEffect(Unit) {
        scanHistoryVM.fetch(context)
    }

    PScaffold(
        topBar = {
            PTopAppBar(navController = navController, title = stringResource(R.string.scan_history))
        },
        content = { paddingValues ->
            PullToRefresh(modifier = Modifier.padding(top = paddingValues.calculateTopPadding()), refreshLayoutState = refreshState) {
                if (itemsState.isNotEmpty()) {
                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                    ) {
                        item {
                            TopSpace()
                        }
                        items(itemsState) { m ->
                            ScanHistoryItem(
                                context = context,
                                text = m,
                                onDelete = {
                                    DialogHelper.confirmToDelete {
                                        scanHistoryVM.delete(context, m)
                                    }
                                }
                            )
                            VerticalSpace(dp = 8.dp)
                        }
                        item {
                            BottomSpace(paddingValues)
                        }
                    }
                } else {
                    NoDataColumn()
                }
            }
        },
    )
}
