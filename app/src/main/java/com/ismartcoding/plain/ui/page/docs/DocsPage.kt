package com.ismartcoding.plain.ui.page.docs

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.features.PermissionsResultEvent
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.preference.DocSortByPreference
import com.ismartcoding.plain.ui.base.ActionButtonMoreWithMenu
import com.ismartcoding.plain.ui.base.ActionButtonSearch
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.NeedPermissionColumn
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PDropdownMenuItemSelect
import com.ismartcoding.plain.ui.base.PDropdownMenuItemSort
import com.ismartcoding.plain.ui.base.PMiniOutlineButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.RadioDialog
import com.ismartcoding.plain.ui.base.RadioDialogOption
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.DocItem
import com.ismartcoding.plain.ui.models.DocsViewModel
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.isAllSelected
import com.ismartcoding.plain.ui.models.toggleSelectAll
import com.ismartcoding.plain.ui.models.toggleSelectMode
import com.ismartcoding.plain.ui.page.RouteName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DocsPage(
    navController: NavHostController,
    viewModel: DocsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val view = LocalView.current
    val window = (view.context as Activity).window
    val itemsState by viewModel.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollState = rememberLazyListState()
    var hasPermission by remember {
        mutableStateOf(AppFeatureType.FILES.hasPermission(context))
    }
    val events by remember { mutableStateOf<MutableList<Job>>(arrayListOf()) }

    val topRefreshLayoutState =
        rememberRefreshLayoutState {
            scope.launch {
                withIO { viewModel.loadAsync(context) }
                setRefreshState(RefreshContentState.Finished)
            }
        }

    LaunchedEffect(Unit) {
        if (hasPermission) {
            scope.launch(Dispatchers.IO) {
                viewModel.sortBy.value = DocSortByPreference.getValueAsync(context)
                viewModel.loadAsync(context)
            }
        }
        events.add(
            receiveEventHandler<PermissionsResultEvent> {
                hasPermission = AppFeatureType.FILES.hasPermission(context)
                scope.launch(Dispatchers.IO) {
                    viewModel.loadAsync(context)
                }
            })
    }

    val insetsController = WindowCompat.getInsetsController(window, view)
    LaunchedEffect(viewModel.selectMode.value) {
        if (viewModel.selectMode.value) {
            insetsController.hide(WindowInsetsCompat.Type.navigationBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.navigationBars())
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            insetsController.show(WindowInsetsCompat.Type.navigationBars())
            events.forEach { it.cancel() }
            events.clear()
        }
    }

    BackHandler(enabled = viewModel.selectMode.value) {
        viewModel.exitSelectMode()
    }

    ViewFileBottomSheet(viewModel)

    val pageTitle = if (viewModel.selectMode.value) {
        LocaleHelper.getStringF(R.string.x_selected, "count", viewModel.selectedIds.size)
    } else {
        stringResource(id = R.string.docs) + " (${itemsState.size})"
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
                        DocSortByPreference.putAsync(context, it)
                        viewModel.sortBy.value = it
                        viewModel.loadAsync(context)
                    }
                }
            },
        ) {
            viewModel.showSortDialog.value = false
        }
    }

    PScaffold(
        navController,
        topBarTitle = pageTitle,
        topBarOnDoubleClick = {
            scope.launch {
                scrollState.scrollToItem(0)
            }
        },
        navigationIcon = {
            if (viewModel.selectMode.value) {
                NavigationCloseIcon {
                    viewModel.exitSelectMode()
                }
            } else {
                NavigationBackIcon {
                    navController.popBackStack()
                }
            }
        },
        actions = {
            if (!hasPermission) {
                return@PScaffold
            }
            if (viewModel.selectMode.value) {
                PMiniOutlineButton(
                    text = stringResource(if (viewModel.isAllSelected()) R.string.unselect_all else R.string.select_all),
                    onClick = {
                        viewModel.toggleSelectAll()
                    },
                )
                HorizontalSpace(dp = 8.dp)
            } else {
                ActionButtonSearch {
                    navController.navigate("${RouteName.DOCS.name}/search?q=")
                }
                ActionButtonMoreWithMenu { dismiss ->
                    PDropdownMenuItemSelect(onClick = {
                        dismiss()
                        viewModel.toggleSelectMode()
                    })
                    PDropdownMenuItemSort(onClick = {
                        dismiss()
                        viewModel.showSortDialog.value = true
                    })
                }
            }

        },
        bottomBar = {
            AnimatedVisibility(
                visible = viewModel.selectMode.value,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }) {
                FilesSelectModeBottomActions(viewModel)
            }
        },
    ) {
        if (!hasPermission) {
            NeedPermissionColumn(AppFeatureType.FILES.getPermission()!!)
            return@PScaffold
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
                            .fillMaxSize(),
                        state = scrollState,
                    ) {
                        item {
                            TopSpace()
                        }
                        items(itemsState) { m ->
                            DocItem(navController, viewModel, m)
                            VerticalSpace(dp = 8.dp)
                        }
                        item {
                            if (itemsState.isNotEmpty() && !viewModel.noMore.value) {
                                LaunchedEffect(Unit) {
                                    scope.launch(Dispatchers.IO) {
                                        withIO { viewModel.moreAsync(context) }
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


