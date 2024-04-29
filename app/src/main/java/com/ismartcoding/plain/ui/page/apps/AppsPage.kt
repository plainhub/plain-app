package com.ismartcoding.plain.ui.page.apps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.preference.PackageSortByPreference
import com.ismartcoding.plain.ui.base.ActionButtonSearch
import com.ismartcoding.plain.ui.base.ActionButtonSort
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.RadioDialog
import com.ismartcoding.plain.ui.base.RadioDialogOption
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.PackageListItem
import com.ismartcoding.plain.ui.models.AppsViewModel
import com.ismartcoding.plain.ui.page.RouteName
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppsPage(
    navController: NavHostController,
    viewModel: AppsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val itemsState by viewModel.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val filtersScrollState = rememberScrollState()

    val topRefreshLayoutState =
        rememberRefreshLayoutState {
            scope.launch {
                withIO { viewModel.loadAsync() }
                setRefreshState(RefreshContentState.Finished)
            }
        }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            viewModel.loadAsync()
        }
    }

    if (viewModel.showSortDialog.value) {
        RadioDialog(
            title = stringResource(R.string.sort),
            options =
            FileSortBy.entries.map {
                RadioDialogOption(
                    text = stringResource(id = it.getTextId()),
                    selected = it == viewModel.sortBy.value,
                ) {
                    scope.launch(Dispatchers.IO) {
                        PackageSortByPreference.putAsync(context, it)
                        viewModel.sortBy.value = it
                        viewModel.loadAsync()
                    }
                }
            },
        ) {
            viewModel.showSortDialog.value = false
        }
    }

    PScaffold(
        navController,
        topBarTitle = stringResource(id = R.string.apps),
        actions = {
            ActionButtonSearch {
                navController.navigate("${RouteName.APPS.name}/search?q=")
            }
            ActionButtonSort {
                viewModel.showSortDialog.value = true
            }
        }
    ) {
        if (!viewModel.showLoading.value) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .horizontalScroll(filtersScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PFilterChip(
                    selected = viewModel.appType.value.isEmpty(),
                    onClick = {
                        viewModel.appType.value = ""
                        scope.launch(Dispatchers.IO) {
                            viewModel.loadAsync()
                        }
                    },
                    label = { Text(stringResource(id = R.string.all) + " (${viewModel.total.value})") }
                )
                PFilterChip(
                    selected = viewModel.appType.value == "system",
                    onClick = {
                        viewModel.appType.value = "system"
                        scope.launch(Dispatchers.IO) {
                            viewModel.loadAsync()
                        }
                    },
                    label = { Text(stringResource(id = R.string.app_type_system) + " (${viewModel.totalSystem.value})") }
                )
                PFilterChip(
                    selected = viewModel.appType.value == "user",
                    onClick = {
                        viewModel.appType.value = "user"
                        scope.launch(Dispatchers.IO) {
                            viewModel.loadAsync()
                        }
                    },
                    label = { Text(stringResource(id = R.string.app_type_user) + " (${viewModel.total.value - viewModel.totalSystem.value})") }
                )
            }
        }
        PullToRefresh(
            refreshLayoutState = topRefreshLayoutState,
        ) {
            AnimatedVisibility(
                visible = true,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                if (itemsState.isNotEmpty()) {
                    LazyColumn(
                        Modifier
                            .fillMaxWidth()
                            .fillMaxHeight(),
                    ) {
                        item {
                            TopSpace()
                        }
                        items(itemsState, key = {
                            it.id
                        }) { m ->
                            PackageListItem(
                                item = m,
                                modifier = PlainTheme.getCardModifier(),
                                onClick = {
                                    navController.navigate("${RouteName.APPS.name}/${m.id}")
                                }
                            )
                            VerticalSpace(dp = 8.dp)
                        }
                        item {
                            if (itemsState.isNotEmpty() && !viewModel.noMore.value) {
                                LaunchedEffect(Unit) {
                                    scope.launch(Dispatchers.IO) {
                                        withIO { viewModel.moreAsync() }
                                    }
                                }
                            }
                            LoadMoreRefreshContent(viewModel.noMore.value)
                        }
                    }
                } else {
                    NoDataColumn(loading = viewModel.showLoading.value)
                }
            }
        }
    }
}


