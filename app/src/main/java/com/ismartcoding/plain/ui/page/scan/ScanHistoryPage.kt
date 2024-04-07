package com.ismartcoding.plain.ui.page.scan

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.DragAnchors
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PSwipeBox
import com.ismartcoding.plain.ui.base.SwipeActionButton
import com.ismartcoding.plain.ui.base.TextCard
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.models.ScanHistoryViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ScanHistoryPage(
    navController: NavHostController,
    viewModel: ScanHistoryViewModel = viewModel(),
) {
    val context = LocalContext.current
    val itemsState by viewModel.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val refreshState =
        rememberRefreshLayoutState {
            viewModel.fetch(context)
            setRefreshState(RefreshContentState.Finished)
        }

    LaunchedEffect(Unit) {
        viewModel.fetch(context)
    }

    PScaffold(
        navController,
        topBarTitle = stringResource(id = R.string.scan_history),
        content = {
            TopSpace()
            PullToRefresh(refreshLayoutState = refreshState) {
                if (itemsState.isNotEmpty()) {
                    LazyColumn(
                        Modifier
                            .fillMaxSize()
                    ) {
                        items(itemsState) { m ->
                            PSwipeBox(
                                modifier = Modifier
                                    .fillMaxWidth(),
                                endContent = { state ->
                                    SwipeActionButton(
                                        text = stringResource(R.string.delete),
                                        color = colorResource(id = R.color.red),
                                        onClick = {
                                            scope.launch {
                                                state.animateTo(DragAnchors.Center)
                                            }
                                            viewModel.delete(context, m)
                                        })
                                    HorizontalSpace(dp = 32.dp)
                                }
                            ) {
                                TextCard(context, text = m)
                            }
                            VerticalSpace(dp = 8.dp)
                        }
                        item {
                            BottomSpace()
                        }
                    }
                } else {
                    NoDataColumn()
                }
            }
        },
    )
}
