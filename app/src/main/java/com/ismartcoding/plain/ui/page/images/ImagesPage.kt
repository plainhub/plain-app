package com.ismartcoding.plain.ui.page.images

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Folder
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
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
import com.ismartcoding.plain.preference.ImageSortByPreference
import com.ismartcoding.plain.ui.base.ActionButtonMoreWithMenu
import com.ismartcoding.plain.ui.base.ActionButtonSearch
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.NeedPermissionColumn
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PDropdownMenuItemCast
import com.ismartcoding.plain.ui.base.PDropdownMenuItemSelect
import com.ismartcoding.plain.ui.base.PDropdownMenuItemSort
import com.ismartcoding.plain.ui.base.PDropdownMenuItemTags
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PMiniOutlineButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.RadioDialog
import com.ismartcoding.plain.ui.base.RadioDialogOption
import com.ismartcoding.plain.ui.base.pinchzoomgrid.PinchZoomGridLayout
import com.ismartcoding.plain.ui.base.pinchzoomgrid.rememberPinchZoomGridState
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.ImageGridItem
import com.ismartcoding.plain.ui.extensions.navigate
import com.ismartcoding.plain.ui.extensions.navigateTags
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.isAllSelected
import com.ismartcoding.plain.ui.models.toggleSelectAll
import com.ismartcoding.plain.ui.models.toggleSelectMode
import com.ismartcoding.plain.ui.page.RouteName
import com.ismartcoding.plain.ui.theme.PlainTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun ImagesPage(
    navController: NavHostController,
    viewModel: ImagesViewModel = viewModel(),
    tagsViewModel: TagsViewModel = viewModel(),
) {
    val context = LocalContext.current
    val view = LocalView.current
    val window = (view.context as Activity).window
    val itemsState by viewModel.itemsFlow.collectAsState()
    val tagsState by tagsViewModel.itemsFlow.collectAsState()
    val tagsMapState by tagsViewModel.tagsMapFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val filtersScrollState = rememberScrollState()
    val scrollState = rememberLazyGridState()
    var hasPermission by remember {
        mutableStateOf(AppFeatureType.FILES.hasPermission(context))
    }
    var lastCellIndex by remember { mutableIntStateOf(3) }
    var canScroll by rememberSaveable { mutableStateOf(true) }

    val pinchState = rememberPinchZoomGridState(
        cellsList = PlainTheme.cellsList,
        initialCellsIndex = lastCellIndex
    )
    LaunchedEffect(pinchState.isZooming) {
        canScroll = !pinchState.isZooming
        lastCellIndex = PlainTheme.cellsList.indexOf(pinchState.currentCells)
    }

    val events by remember { mutableStateOf<MutableList<Job>>(arrayListOf()) }

    val topRefreshLayoutState =
        rememberRefreshLayoutState {
            scope.launch {
                withIO { viewModel.loadAsync(context, tagsViewModel) }
                setRefreshState(RefreshContentState.Finished)
            }
        }

    LaunchedEffect(Unit) {
        tagsViewModel.dataType.value = viewModel.dataType
        if (hasPermission) {
            scope.launch(Dispatchers.IO) {
                viewModel.sortBy.value = ImageSortByPreference.getValueAsync(context)
                viewModel.loadAsync(context, tagsViewModel)
            }
        }
        events.add(
            receiveEventHandler<PermissionsResultEvent> {
                hasPermission = AppFeatureType.FILES.hasPermission(context)
                scope.launch(Dispatchers.IO) {
                    viewModel.sortBy.value = ImageSortByPreference.getValueAsync(context)
                    viewModel.loadAsync(context, tagsViewModel)
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

    ViewImageBottomSheet(viewModel, tagsViewModel, tagsMapState, tagsState)

    val pageTitle = if (viewModel.selectMode.value) {
        LocaleHelper.getStringF(R.string.x_selected, "count", viewModel.selectedIds.size)
    } else if (viewModel.tag.value != null) {
        stringResource(id = R.string.images) + " - " + viewModel.tag.value!!.name
    } else if (viewModel.trash.value) {
        stringResource(id = R.string.images) + " - " + stringResource(id = R.string.trash)
    } else {
        stringResource(id = R.string.images)
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
                        ImageSortByPreference.putAsync(context, it)
                        viewModel.sortBy.value = it
                        viewModel.loadAsync(context, tagsViewModel)
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
                if (viewModel.bucket.value == null) {
                    PIconButton(
                        icon = Icons.Outlined.Folder,
                        contentDescription = stringResource(R.string.folder),
                        tint = MaterialTheme.colorScheme.onSurface,
                    ) {
                        navController.navigate(RouteName.IMAGES)
                    }
                }
                ActionButtonMoreWithMenu { dismiss ->
                    PDropdownMenuItemSelect(onClick = {
                        dismiss()
                        viewModel.toggleSelectMode()
                    })
                    PDropdownMenuItemTags(onClick = {
                        dismiss()
                        navController.navigateTags(viewModel.dataType)
                    })
                    PDropdownMenuItemSort(onClick = {
                        dismiss()
                        viewModel.showSortDialog.value = true
                    })
                    PDropdownMenuItemCast(onClick = {
                        dismiss()
                    })
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = viewModel.selectMode.value,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }) {
                FilesSelectModeBottomActions(viewModel, tagsViewModel, tagsState)
            }
        },
    ) {
        if (!hasPermission) {
            NeedPermissionColumn(AppFeatureType.FILES.getPermission()!!)
            return@PScaffold
        }

        if (!viewModel.selectMode.value) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .horizontalScroll(filtersScrollState),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                PFilterChip(
                    selected = !viewModel.trash.value && viewModel.tag.value == null,
                    onClick = {
                        viewModel.trash.value = false
                        viewModel.tag.value = null
                        scope.launch(Dispatchers.IO) {
                            viewModel.loadAsync(context, tagsViewModel)
                        }
                    },
                    label = { Text(stringResource(id = R.string.all) + " (${viewModel.total.value})") }
                )
//                PFilterChip(
//                    selected = viewModel.trash.value,
//                    onClick = {
//                        viewModel.trash.value = true
//                        viewModel.tag.value = null
//                        scope.launch(Dispatchers.IO) {
//                            viewModel.loadAsync(context, tagsViewModel)
//                        }
//                    },
//                    label = { Text(stringResource(id = R.string.trash) + " (${viewModel.totalTrash.value})") }
//                )
                tagsState.forEach { tag ->
                    PFilterChip(
                        selected = viewModel.tag.value?.id == tag.id,
                        onClick = {
                            viewModel.trash.value = false
                            viewModel.tag.value = tag
                            scope.launch(Dispatchers.IO) {
                                viewModel.loadAsync(context, tagsViewModel)
                            }
                        },
                        label = { Text("${tag.name} (${tag.count})") }
                    )
                }
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
                    PinchZoomGridLayout(state = pinchState) {
                        LazyVerticalGrid(
                            state = gridState,
                            modifier = Modifier.fillMaxSize(),
                            columns = gridCells,
                            userScrollEnabled = canScroll,
                            horizontalArrangement = Arrangement.spacedBy(1.dp),
                            verticalArrangement = Arrangement.spacedBy(1.dp),
                        ) {
                            items(itemsState,
                                key = {
                                    it.id
                                },
                                contentType = {
                                    "image"
                                },
                                span = {
                                    GridItemSpan(1)
                                }) { m ->
                                ImageGridItem(
                                    modifier = Modifier
                                        .pinchItem(key = m.id)
                                        .animateItemPlacement(),
                                    navController, viewModel, m
                                )
                            }
                            item(
                                span = { GridItemSpan(maxLineSpan) },
                                key = "loadMore"
                            ) {
                                if (itemsState.isNotEmpty() && !viewModel.noMore.value) {
                                    LaunchedEffect(Unit) {
                                        scope.launch(Dispatchers.IO) {
                                            withIO { viewModel.moreAsync(context, tagsViewModel) }
                                        }
                                    }
                                }
                                LoadMoreRefreshContent(viewModel.noMore.value)
                            }
                            item(
                                span = { GridItemSpan(maxLineSpan) },
                                key = "bottomSpace"
                            ) {
                                BottomSpace()
                            }
                        }
                    }
                } else {
                    NoDataColumn(loading = viewModel.showLoading.value)
                }
            }
        }
    }
}


