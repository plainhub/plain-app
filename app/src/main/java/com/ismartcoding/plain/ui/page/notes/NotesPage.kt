package com.ismartcoding.plain.ui.page.notes

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.animateTo
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Label
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.enums.ActionSourceType
import com.ismartcoding.plain.data.enums.DataType
import com.ismartcoding.plain.db.DNote
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.ActionButtonMore
import com.ismartcoding.plain.ui.base.ActionButtonSearch
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.DragAnchors
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PDraggableElement
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
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
import com.ismartcoding.plain.ui.note.NoteDialog
import com.ismartcoding.plain.ui.page.RouteName
import com.ismartcoding.plain.ui.theme.bottomAppBarContainer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesPage(
    navController: NavHostController,
    viewModel: NotesViewModel = viewModel(),
    tagsViewModel: TagsViewModel = viewModel(),
) {
    val itemsState by viewModel.itemsFlow.collectAsState()
    val tagsState by tagsViewModel.itemsFlow.collectAsState()
    val tagsMapState by tagsViewModel.tagsMapFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val filtersScrollState = rememberScrollState()
    var showActionBottomSheet by remember { mutableStateOf(false) }
    var selectedItem by remember { mutableStateOf<DNote?>(null) }
    var isMenuOpen by remember { mutableStateOf(false) }
    val view = LocalView.current
    val events by remember { mutableStateOf<MutableList<Job>>(arrayListOf()) }

    val topRefreshLayoutState =
        rememberRefreshLayoutState {
            scope.launch {
                withIO {
                    viewModel.loadAsync(tagsViewModel)
                }
                setRefreshState(RefreshContentState.Stop)
            }
        }

    LaunchedEffect(Unit) {
        tagsViewModel.dataType.value = DataType.NOTE
        events.add(
            receiveEventHandler<ActionEvent> { event ->
                if (event.source == ActionSourceType.NOTE) {
                    scope.launch(Dispatchers.IO) {
                        viewModel.loadAsync(tagsViewModel)
                    }
                }
            }
        )
        scope.launch(Dispatchers.IO) {
            viewModel.loadAsync(tagsViewModel)
        }
    }

    val backgroundColor = MaterialTheme.colorScheme.background
    val bottomAppBarContainerColor = MaterialTheme.colorScheme.bottomAppBarContainer()
    LaunchedEffect(viewModel.selectMode) {
        val window = (view.context as Activity).window
        window.navigationBarColor = if (viewModel.selectMode) bottomAppBarContainerColor.toArgb() else backgroundColor.toArgb()
    }

    val pageTitle = if (viewModel.selectMode) {
        LocaleHelper.getStringF(R.string.x_selected, "count", viewModel.selectedIds.size)
    } else if (viewModel.tag.value != null) {
        "${viewModel.tag.value!!.name} (${viewModel.tag.value!!.count})"
    } else {
        LocaleHelper.getStringF(if (viewModel.trash.value) R.string.trash_title else R.string.notes_title, "count", viewModel.total.value)
    }

    if (showActionBottomSheet) {
        ItemActionBottomSheet(
            viewModel,
            tagsViewModel,
            m = selectedItem!!,
            tagsMapState,
            tagsState,
            onDismiss = {
                showActionBottomSheet = false
                selectedItem = null
            }
        )
    }

    BackHandler(enabled = viewModel.selectMode) {
        viewModel.exitSelectMode()
    }


    PScaffold(
        navController,
        topBarTitle = pageTitle,
        navigationIcon = {
            if (viewModel.selectMode) {
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
            if (viewModel.selectMode) {
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
                ActionButtonMore {
                    isMenuOpen = !isMenuOpen
                }
                PDropdownMenu(
                    expanded = isMenuOpen,
                    onDismissRequest = { isMenuOpen = false },
                    content = {
                        PDropdownMenuItem(text = { Text(stringResource(R.string.select)) }, leadingIcon = {
                            Icon(
                                Icons.Outlined.Checklist,
                                contentDescription = stringResource(id = R.string.select)
                            )
                        }, onClick = {
                            isMenuOpen = false
                            viewModel.toggleSelectMode()
                        })
                        PDropdownMenuItem(text = {
                            Text(stringResource(R.string.tags))
                        }, leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Outlined.Label,
                                contentDescription = stringResource(id = R.string.tags)
                            )
                        }, onClick = {
                            isMenuOpen = false
                            navController.navigate("${RouteName.TAGS.name}?dataType=${DataType.NOTE.value}")
                        })
                    })
            }
        },
        bottomBar = if (viewModel.selectMode) {
            {
                SelectModeBottomAppBar(viewModel, tagsViewModel, tagsState)
            }
        } else null,
        floatingActionButton = if (viewModel.selectMode) null else {
            {
                PDraggableElement {
                    FloatingActionButton(
                        onClick = {
                            scope.launch {
                                NoteDialog().show(null, viewModel.tag.value)
                            }
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
        if (!viewModel.selectMode) {
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
                    label = { Text(stringResource(id = R.string.all)) }
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
                    label = { Text(stringResource(id = R.string.trash)) }
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
                            .fillMaxWidth()
                            .fillMaxHeight(),
                    ) {
                        item {
                            TopSpace()
                        }
                        items(itemsState) { m ->
                            PSwipeBox(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .wrapContentHeight(),
                                enabled = !viewModel.selectMode,
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
                                    selectedItem,
                                    onClick = {
                                        if (viewModel.selectMode) {
                                            viewModel.select(m.id)
                                        } else {
                                            NoteDialog().show(m)
                                        }
                                    },
                                    onLongClick = {
                                        if (viewModel.selectMode) {
                                            return@NoteListItem
                                        }
                                        selectedItem = m
                                        showActionBottomSheet = true
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
