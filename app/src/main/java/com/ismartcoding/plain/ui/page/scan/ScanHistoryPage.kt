package com.ismartcoding.plain.ui.page.scan

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.TextCard
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.ScanHistoryViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScanHistoryPage(
    navController: NavHostController,
    viewModel: ScanHistoryViewModel = viewModel(),
) {
    val context = LocalContext.current
    val itemsState by viewModel.itemsFlow.collectAsState()

    val refreshState = rememberRefreshLayoutState {
        viewModel.fetch(context)
        setRefreshState(RefreshContentState.Stop)
    }

    LaunchedEffect(Unit) {
        viewModel.fetch(context)
    }

    PScaffold(
        navController,
        topBarTitle = stringResource(id = R.string.scan_history),
        content = {
            PullToRefresh(refreshLayoutState = refreshState) {
                if (itemsState.isNotEmpty()) {
                    LazyColumn(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                    ) {
                        items(itemsState) { m ->
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .padding(start = 32.dp, end = 16.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Spacer(modifier = Modifier.weight(1f))
                                PIconButton(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = stringResource(R.string.delete),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                ) {
                                    DialogHelper.confirmToAction(context, R.string.confirm_to_delete) {
                                        viewModel.delete(context, m)
                                    }
                                }
                            }
                            TextCard(context, text = m)
                        }
                        item {
                            BottomSpace()
                        }
                    }
                } else {
                    NoDataColumn()
                }
            }
        }
    )
}

