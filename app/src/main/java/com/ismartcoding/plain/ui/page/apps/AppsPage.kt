package com.ismartcoding.plain.ui.page.apps

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.VerticalRefreshableLayout
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.PackageListItem
import com.ismartcoding.plain.ui.models.AppsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsPage(
    navController: NavHostController,
    viewModel: AppsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val itemsState by viewModel.itemsFlow.collectAsState()

    val topRefreshLayoutState =
        rememberRefreshLayoutState {
            viewModel.load()
            setRefreshState(RefreshContentState.Stop)
        }

    val bottomRefreshLayoutState =
        rememberRefreshLayoutState {
            viewModel.loadMore()
            setRefreshState(RefreshContentState.Stop)
        }

    LaunchedEffect(Unit) {
        viewModel.load()
    }

    PScaffold(
        navController,
        topBarTitle = stringResource(id = R.string.apps),
        content = {
            VerticalRefreshableLayout(
                topRefreshLayoutState = topRefreshLayoutState,
                bottomRefreshLayoutState = bottomRefreshLayoutState,
                bottomNoMore = viewModel.noMore.value
            ) {
                if (itemsState.isNotEmpty()) {
                    LazyColumn(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                    ) {
                        items(itemsState) { m ->
                            PackageListItem(
                                item = m
                            )
                        }
                    }
                } else {
                    NoDataColumn(loading = viewModel.showLoading.value)
                }
            }
        },
    )
}


