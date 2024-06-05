package com.ismartcoding.plain.ui.page.feeds

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Upload
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.queryOpenableFileName
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.contentResolver
import com.ismartcoding.plain.enums.ExportFileType
import com.ismartcoding.plain.enums.PickFileTag
import com.ismartcoding.plain.enums.PickFileType
import com.ismartcoding.plain.extensions.formatName
import com.ismartcoding.plain.features.ExportFileEvent
import com.ismartcoding.plain.features.ExportFileResultEvent
import com.ismartcoding.plain.features.PickFileEvent
import com.ismartcoding.plain.features.PickFileResultEvent
import com.ismartcoding.plain.features.feed.FeedHelper
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.ActionButtonMoreWithMenu
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PDraggableElement
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.base.PDropdownMenuItemSelect
import com.ismartcoding.plain.ui.base.PMiniOutlineButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.FeedListItem
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.FeedsViewModel
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.isAllSelected
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.models.showBottomActions
import com.ismartcoding.plain.ui.models.toggleSelectAll
import com.ismartcoding.plain.ui.models.toggleSelectMode
import com.ismartcoding.plain.ui.nav.RouteName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.util.Date

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun FeedsPage(
    navController: NavHostController,
    viewModel: FeedsViewModel = viewModel(),
) {
    val view = LocalView.current
    val window = (view.context as Activity).window
    val itemsState by viewModel.itemsFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val events by remember { mutableStateOf<MutableList<Job>>(arrayListOf()) }

    val topRefreshLayoutState =
        rememberRefreshLayoutState {
            scope.launch {
                withIO { viewModel.loadAsync(withCount = true) }
                setRefreshState(RefreshContentState.Finished)
            }
        }

    LaunchedEffect(Unit) {
        scope.launch(Dispatchers.IO) {
            viewModel.loadAsync(withCount = true)
        }
        events.add(
            receiveEventHandler<PickFileResultEvent> { event ->
                if (event.tag != PickFileTag.FEED) {
                    return@receiveEventHandler
                }

                val uri = event.uris.first()
                InputStreamReader(contentResolver.openInputStream(uri)!!).use { reader ->
                    DialogHelper.showLoading()
                    withIO {
                        try {
                            FeedHelper.importAsync(reader)
                            viewModel.loadAsync()
                            DialogHelper.hideLoading()
                        } catch (ex: Exception) {
                            DialogHelper.hideLoading()
                            DialogHelper.showMessage(ex.toString())
                        }
                    }
                }
            },
        )

        events.add(
            receiveEventHandler<ExportFileResultEvent> { event ->
                if (event.type == ExportFileType.OPML) {
                    OutputStreamWriter(contentResolver.openOutputStream(event.uri)!!, Charsets.UTF_8).use { writer ->
                        withIO { FeedHelper.exportAsync(writer) }
                    }
                    val fileName = contentResolver.queryOpenableFileName(event.uri)
                    DialogHelper.showConfirmDialog(
                        "",
                        LocaleHelper.getStringF(R.string.exported_to, "name", fileName),
                    )
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

    AddFeedDialog(viewModel)
    EditFeedDialog(viewModel)
    ViewFeedBottomSheet(viewModel)

    val pageTitle = if (viewModel.selectMode.value) {
        LocaleHelper.getStringF(R.string.x_selected, "count", viewModel.selectedIds.size)
    } else {
        LocaleHelper.getStringF(R.string.subscriptions_title, "count", itemsState.size)
    }

    PScaffold(
        topBar = {
            PTopAppBar(
                navController = navController,
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
                title = pageTitle,
                actions = {
                    if (viewModel.selectMode.value) {
                        PMiniOutlineButton(
                            text = stringResource(if (viewModel.isAllSelected()) R.string.unselect_all else R.string.select_all),
                            onClick = {
                                viewModel.toggleSelectAll()
                            },
                        )
                        HorizontalSpace(dp = 8.dp)
                    } else {
                        ActionButtonMoreWithMenu { dismiss ->
                            PDropdownMenuItemSelect(onClick = {
                                dismiss()
                                viewModel.toggleSelectMode()
                            })
                            PDropdownMenuItem(text = { Text(stringResource(R.string.import_opml_file)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Upload,
                                        contentDescription = stringResource(id = R.string.import_opml_file)
                                    )
                                }, onClick = {
                                    dismiss()
                                    sendEvent(PickFileEvent(PickFileTag.FEED, PickFileType.FILE, false))
                                })

                            PDropdownMenuItem(text = { Text(stringResource(R.string.export_opml_file)) },
                                leadingIcon = {
                                    Icon(
                                        Icons.Outlined.Download,
                                        contentDescription = stringResource(id = R.string.export_opml_file)
                                    )
                                }, onClick = {
                                    dismiss()
                                    sendEvent(ExportFileEvent(ExportFileType.OPML, "feeds_" + Date().formatName() + ".opml"))
                                })
                        }
                    }
                },
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = viewModel.showBottomActions(),
                enter = slideInVertically { it },
                exit = slideOutVertically { it }) {
                FeedsSelectModeBottomActions(viewModel)
            }
        },
        floatingActionButton = if (viewModel.selectMode.value) null else {
            {
                PDraggableElement {
                    FloatingActionButton(
                        onClick = {
                            viewModel.showAddDialog()
                        },
                    ) {
                        Icon(
                            Icons.Outlined.Add,
                            stringResource(R.string.add),
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
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
                            .fillMaxSize()
                    ) {
                        item {
                            TopSpace()
                        }
                        items(itemsState) { m ->
                            FeedListItem(
                                viewModel = viewModel,
                                m,
                                onClick = {
                                    if (viewModel.selectMode.value) {
                                        viewModel.select(m.id)
                                    } else {
                                        navController.navigate("${RouteName.FEED_ENTRIES.name}?feedId=${m.id}")
                                    }
                                },
                                onLongClick = {
                                    if (viewModel.selectMode.value) {
                                        return@FeedListItem
                                    }
                                    viewModel.selectedItem.value = m
                                },
                            )
                            VerticalSpace(dp = 8.dp)
                        }
                        item {
                            VerticalSpace(dp = paddingValues.calculateBottomPadding())
                        }
                    }
                } else {
                    NoDataColumn(loading = viewModel.showLoading.value)
                }
            }
        }
    }
}
