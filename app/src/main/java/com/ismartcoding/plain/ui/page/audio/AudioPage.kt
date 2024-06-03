package com.ismartcoding.plain.ui.page.audio

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
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
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.preference.AudioSortByPreference
import com.ismartcoding.plain.ui.base.ActionButtonMoreWithMenu
import com.ismartcoding.plain.ui.base.ActionButtonSearch
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.NeedPermissionColumn
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PDropdownMenuItemSelect
import com.ismartcoding.plain.ui.base.PDropdownMenuItemTags
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PMiniOutlineButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.AudioListItem
import com.ismartcoding.plain.ui.nav.navigateTags
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.isAllSelected
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.models.showBottomActions
import com.ismartcoding.plain.ui.models.toggleSelectAll
import com.ismartcoding.plain.ui.models.toggleSelectMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AudioPage(
    navController: NavHostController,
    viewModel: AudioViewModel = viewModel(),
    tagsViewModel: TagsViewModel = viewModel(),
) {
    val view = LocalView.current
    val window = (view.context as Activity).window
    val itemsState by viewModel.itemsFlow.collectAsState()
    val tagsState by tagsViewModel.itemsFlow.collectAsState()
    val tagsMapState by tagsViewModel.tagsMapFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val filtersScrollState = rememberScrollState()
    val scrollState = rememberLazyListState()
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(AppFeatureType.FILES.hasPermission(context))
    }
    val events by remember { mutableStateOf<MutableList<Job>>(arrayListOf()) }

    val topRefreshLayoutState =
        rememberRefreshLayoutState {
            scope.launch {
                withIO {
                    viewModel.loadAsync(context, tagsViewModel)
                }
                setRefreshState(RefreshContentState.Finished)
            }
        }

    LaunchedEffect(Unit) {
        tagsViewModel.dataType.value = viewModel.dataType
        if (hasPermission) {
            scope.launch(Dispatchers.IO) {
                viewModel.sortBy.value = AudioSortByPreference.getValueAsync(context)
                viewModel.loadAsync(context, tagsViewModel)
            }
        }
        events.add(
            receiveEventHandler<PermissionsResultEvent> {
                hasPermission = AppFeatureType.FILES.hasPermission(context)
                scope.launch(Dispatchers.IO) {
                    viewModel.sortBy.value = AudioSortByPreference.getValueAsync(context)
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

    val pageTitle = if (viewModel.selectMode.value) {
        LocaleHelper.getStringF(R.string.x_selected, "count", viewModel.selectedIds.size)
    } else if (viewModel.tag.value != null) {
        stringResource(id = R.string.audios) + " - " + viewModel.tag.value!!.name
    } else {
        stringResource(id = R.string.audios)
    }

    ViewAudioBottomSheet(
        viewModel,
        tagsViewModel,
        tagsMapState,
        tagsState,
    )

    BackHandler(enabled = viewModel.selectMode.value) {
        viewModel.exitSelectMode()
    }

    PScaffold(
        topBar = {
            PTopAppBar(modifier = Modifier.combinedClickable(
                onClick = {},
                onDoubleClick = {
                    scope.launch {
                        scrollState.scrollToItem(0)
                    }
                }
            ), navController = navController,
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
                title = pageTitle, actions = {
                    if (!hasPermission) {
                        return@PTopAppBar
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
                        }
                    }
                })
        },

        bottomBar = {
            AnimatedVisibility(
                visible = viewModel.showBottomActions(),
                enter = slideInVertically { it },
                exit = slideOutVertically { it }) {
                SelectModeBottomActions(viewModel, tagsViewModel, tagsState)
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
                    LazyColumn(
                        Modifier
                            .fillMaxSize(),
                        state = scrollState,
                    ) {
                        item {
                            TopSpace()
                        }
                        items(itemsState) { m ->
                            val tagIds = tagsMapState[m.id]?.map { it.tagId } ?: emptyList()
                            AudioListItem(
                                context,
                                viewModel,
                                tagsViewModel,
                                m,
                                tagsState.filter { tagIds.contains(it.id) },
                                onClick = {
                                    if (viewModel.selectMode.value) {
                                        viewModel.select(m.id)
                                    } else {

                                    }
                                },
                                onLongClick = {
                                    if (viewModel.selectMode.value) {
                                        return@AudioListItem
                                    }
                                    viewModel.selectedItem.value = m
                                }
                            )
                            VerticalSpace(dp = 8.dp)
                        }
                        item {
                            if (itemsState.isNotEmpty() && !viewModel.noMore.value) {
                                LaunchedEffect(Unit) {
                                    scope.launch(Dispatchers.IO) {
                                        withIO { viewModel.moreAsync(context, tagsViewModel) }
                                    }
                                }
                            }
                            LoadMoreRefreshContent(viewModel.noMore.value)
                            BottomSpace()
                        }
                    }
                } else {
                    NoDataColumn(loading = viewModel.showLoading.value)
                }
            }
        }
    }
}
