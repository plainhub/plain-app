package com.ismartcoding.plain.ui.page.notes

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.ActionButtonMoreWithMenu
import com.ismartcoding.plain.ui.base.ActionButtonSearch
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.DragAnchors
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PDraggableElement
import com.ismartcoding.plain.ui.base.PDropdownMenuItemSelect
import com.ismartcoding.plain.ui.base.PDropdownMenuItemTags
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PMiniOutlineButton
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PSwipeBox
import com.ismartcoding.plain.ui.base.SwipeActionButton
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.NoteListItem
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.isAllSelected
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.models.toggleSelectAll
import com.ismartcoding.plain.ui.models.toggleSelectMode
import com.ismartcoding.plain.ui.page.RouteName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesPage(
    navController: NavHostController,
    viewModel: NotesViewModel = viewModel(),
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

    val topRefreshLayoutState =
        rememberRefreshLayoutState {
            scope.launch {
                withIO {
                    viewModel.loadAsync(tagsViewModel)
                }
                setRefreshState(RefreshContentState.Finished)
            }
        }

    LaunchedEffect(Unit) {
        tagsViewModel.dataType.value = viewModel.dataType
        scope.launch(Dispatchers.IO) {
            viewModel.loadAsync(tagsViewModel)
        }
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
        }
    }

    val pageTitle = if (viewModel.selectMode.value) {
        LocaleHelper.getStringF(R.string.x_selected, "count", viewModel.selectedIds.size)
    } else if (viewModel.tag.value != null) {
        stringResource(id = R.string.notes) + " - " + viewModel.tag.value!!.name
    } else if (viewModel.trash.value) {
        stringResource(id = R.string.notes) + " - " + stringResource(id = R.string.trash)
    } else {
        stringResource(id = R.string.notes)
    }

    ViewNoteBottomSheet(
        viewModel,
        tagsViewModel,
        tagsMapState,
        tagsState,
    )

    BackHandler(enabled = viewModel.selectMode.value) {
        viewModel.exitSelectMode()
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
                    navController.navigate("${RouteName.NOTES.name}/search?q=")
                }
                ActionButtonMoreWithMenu { dismiss ->
                    PDropdownMenuItemSelect(onClick = {
                        dismiss()
                        viewModel.toggleSelectMode()
                    })
                    PDropdownMenuItemTags(onClick = {
                        dismiss()
                        navController.navigate("${RouteName.TAGS.name}?dataType=${viewModel.dataType.value}")
                    })
                }
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = viewModel.selectMode.value,
                enter = slideInVertically { it },
                exit = slideOutVertically { it }) {
                SelectModeBottomActions(viewModel, tagsViewModel, tagsState)
            }
        },
        floatingActionButton = if (viewModel.selectMode.value) null else {
            {
                PDraggableElement {
                    FloatingActionButton(
                        onClick = {
                            navController.navigate("${RouteName.NOTES.name}/create?tagId=${viewModel.tag.value?.id ?: ""}")
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
    ) {
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
                            viewModel.loadAsync(tagsViewModel)
                        }
                    },
                    label = { Text(stringResource(id = R.string.all) + " (${viewModel.total.value})") }
                )
                PFilterChip(
                    selected = viewModel.trash.value,
                    onClick = {
                        viewModel.trash.value = true
                        viewModel.tag.value = null
                        scope.launch(Dispatchers.IO) {
                            viewModel.loadAsync(tagsViewModel)
                        }
                    },
                    label = { Text(stringResource(id = R.string.trash) + " (${viewModel.totalTrash.value})") }
                )
                tagsState.forEach { tag ->
                    PFilterChip(
                        selected = viewModel.tag.value?.id == tag.id,
                        onClick = {
                            viewModel.trash.value = false
                            viewModel.tag.value = tag
                            scope.launch(Dispatchers.IO) {
                                viewModel.loadAsync(tagsViewModel)
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
                        items(itemsState, key = {
                            it.id
                        }) { m ->
                            PSwipeBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                enabled = !viewModel.selectMode.value,
                                startContent = if (viewModel.trash.value) {
                                    { state ->
                                        HorizontalSpace(dp = 32.dp)
                                        SwipeActionButton(
                                            text = stringResource(R.string.restore),
                                            color = colorResource(id = R.color.blue),
                                            onClick = {
                                                scope.launch {
                                                    state.animateTo(DragAnchors.Center)
                                                }
                                                val ids = setOf(m.id)
                                                viewModel.untrash(ids)
                                            })
                                    }
                                } else null,
                                endContent = { state ->
                                    if (viewModel.trash.value) {
                                        SwipeActionButton(
                                            text = stringResource(R.string.delete),
                                            color = colorResource(id = R.color.red),
                                            onClick = {
                                                scope.launch {
                                                    state.animateTo(DragAnchors.Center)
                                                }
                                                val ids = setOf(m.id)
                                                viewModel.delete(ids)
                                            })
                                    } else {
                                        SwipeActionButton(
                                            text = stringResource(R.string.move_to_trash),
                                            color = colorResource(id = R.color.red),
                                            onClick = {
                                                scope.launch {
                                                    state.animateTo(DragAnchors.Center)
                                                }
                                                val ids = setOf(m.id)
                                                viewModel.trash(ids)
                                            })
                                    }

                                    HorizontalSpace(dp = 32.dp)
                                }
                            ) {
                                val tagIds = tagsMapState[m.id]?.map { it.tagId } ?: emptyList()
                                NoteListItem(
                                    viewModel,
                                    tagsViewModel,
                                    m,
                                    tagsState.filter { tagIds.contains(it.id) },
                                    onClick = {
                                        if (viewModel.selectMode.value) {
                                            viewModel.select(m.id)
                                        } else {
                                            navController.navigate("${RouteName.NOTES.name}/${m.id}")
                                        }
                                    },
                                    onLongClick = {
                                        if (viewModel.selectMode.value) {
                                            return@NoteListItem
                                        }
                                        viewModel.selectedItem.value = m
                                    }
                                )
                            }
                            VerticalSpace(dp = 8.dp)
                        }
                        item {
                            if (itemsState.isNotEmpty() && !viewModel.noMore.value) {
                                LaunchedEffect(Unit) {
                                    scope.launch(Dispatchers.IO) {
                                        withIO { viewModel.moreAsync(tagsViewModel) }
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
