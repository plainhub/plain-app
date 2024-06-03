package com.ismartcoding.plain.ui.page.notes

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.logcat.LogCat
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.ActionButtonMoreWithMenu
import com.ismartcoding.plain.ui.base.ActionButtonSearch
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
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.fastscroll.LazyColumnScrollbar
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.tabs.PScrollableTabRow
import com.ismartcoding.plain.ui.components.ListSearchBar
import com.ismartcoding.plain.ui.components.NoteListItem
import com.ismartcoding.plain.ui.nav.navigateTags
import com.ismartcoding.plain.ui.extensions.reset
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.enterSearchMode
import com.ismartcoding.plain.ui.models.exitSearchMode
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.isAllSelected
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.models.showBottomActions
import com.ismartcoding.plain.ui.models.toggleSelectAll
import com.ismartcoding.plain.ui.models.toggleSelectMode
import com.ismartcoding.plain.ui.nav.RouteName
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
    val scrollStateMap = remember {
        mutableStateMapOf<Int, LazyListState>()
    }
    val pagerState = rememberPagerState(pageCount = { viewModel.tabs.value.size })
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = {
        (scrollStateMap[pagerState.currentPage]?.firstVisibleItemIndex ?: 0) > 0 && !viewModel.selectMode.value
    })
    var isFirstTime by remember { mutableStateOf(true) }

    val topRefreshLayoutState =
        rememberRefreshLayoutState {
            scope.launch {
                withIO {
                    viewModel.loadAsync(tagsViewModel)
                }
                setRefreshState(RefreshContentState.Finished)
            }
        }

    val once = rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        if (!once.value) {
            once.value = true
            tagsViewModel.dataType.value = viewModel.dataType
            scope.launch(Dispatchers.IO) {
                viewModel.loadAsync(tagsViewModel)
            }
        } else {
            // refresh tabs in case tag name changed in tags page
            scope.launch(Dispatchers.IO) {
                viewModel.refreshTabsAsync(tagsViewModel)
            }
        }
    }

    val insetsController = WindowCompat.getInsetsController(window, view)
    LaunchedEffect(viewModel.selectMode.value) {
        if (viewModel.selectMode.value) {
            scrollBehavior.reset()
            insetsController.hide(WindowInsetsCompat.Type.navigationBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.navigationBars())
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (isFirstTime) {
            isFirstTime = false
            return@LaunchedEffect
        }
        when (val index = pagerState.currentPage) {
            0 -> {
                viewModel.trash.value = false
                viewModel.tag.value = null
            }

            1 -> {
                viewModel.trash.value = true
                viewModel.tag.value = null
            }

            else -> {
                viewModel.trash.value = false
                viewModel.tag.value = tagsState.getOrNull(index - 2)
            }
        }
        scope.launch {
            scrollBehavior.reset()
            scrollStateMap[pagerState.currentPage]?.scrollToItem(0)
        }
        scope.launch(Dispatchers.IO) {
            viewModel.loadAsync(tagsViewModel)
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

    val onSearch: (String) -> Unit = {
        viewModel.searchActive.value = false
        viewModel.showLoading.value = true
        scope.launch {
            scrollStateMap[pagerState.currentPage]?.scrollToItem(0)
        }
        scope.launch(Dispatchers.IO) {
            viewModel.loadAsync(tagsViewModel)
        }
    }

    BackHandler(enabled = viewModel.selectMode.value || viewModel.showSearchBar.value) {
        if (viewModel.selectMode.value) {
            viewModel.exitSelectMode()
        } else if (viewModel.showSearchBar.value) {
            if (!viewModel.searchActive.value || viewModel.queryText.value.isEmpty()) {
                viewModel.exitSearchMode()
                onSearch("")
            }
        }
    }

    PScaffold(
        topBar = {
            if (viewModel.showSearchBar.value) {
                ListSearchBar(
                    viewModel = viewModel,
                    onSearch = onSearch
                )
                return@PScaffold
            }
            PTopAppBar(
                modifier = Modifier.combinedClickable(onClick = {}, onDoubleClick = {
                    scope.launch {
                        scrollStateMap[pagerState.currentPage]?.scrollToItem(0)
                    }
                }),
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
                scrollBehavior = scrollBehavior,
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
                            viewModel.enterSearchMode()
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
                },
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = viewModel.showBottomActions(),
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
    ) { paddingValues ->
        if (!viewModel.selectMode.value) {
            PScrollableTabRow(
                selectedTabIndex = pagerState.currentPage,
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                viewModel.tabs.value.forEachIndexed { index, s ->
                    PFilterChip(
                        modifier = Modifier.padding(start = if (index == 0) 0.dp else 8.dp),
                        selected = pagerState.currentPage == index,
                        onClick = {
                            scope.launch {
                                pagerState.scrollToPage(index)
                            }
                        },
                        label = {
                            if (index < 2) {
                                Text(text = s.title + " (" + s.count + ")")
                            } else {
                                Text(if (viewModel.queryText.value.isNotEmpty()) s.title else "${s.title} (${s.count})")
                            }
                        }
                    )
                }
            }
        }

        HorizontalPager(state = pagerState) { index ->
            PullToRefresh(
                refreshLayoutState = topRefreshLayoutState,
            ) {
                AnimatedVisibility(
                    visible = true,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (itemsState.isNotEmpty()) {
                        val scrollState = rememberLazyListState()
                        scrollStateMap[index] = scrollState
                        LazyColumnScrollbar(
                            state = scrollState,
                        ) {
                            LazyColumn(
                                Modifier
                                    .fillMaxSize()
                                    .nestedScroll(scrollBehavior.nestedScrollConnection),
                                state = scrollState,
                            ) {
                                item {
                                    TopSpace()
                                }
                                items(itemsState, key = {
                                    it.id
                                }) { m ->
                                    val tagIds = tagsMapState[m.id]?.map { it.tagId } ?: emptyList()
                                    NoteListItem(
                                        viewModel,
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
                                        },
                                        onClickTag = { tag ->
                                            if (viewModel.selectMode.value) {
                                                return@NoteListItem
                                            }
                                            val idx = viewModel.tabs.value.indexOfFirst { it.value == tag.id }
                                            if (idx != -1) {
                                                scope.launch {
                                                    pagerState.scrollToPage(idx)
                                                }
                                            }
                                        }
                                    )
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
                                }
                                item {
                                    VerticalSpace(dp = paddingValues.calculateBottomPadding())
                                }
                            }
                        }
                    } else {
                        NoDataColumn(loading = viewModel.showLoading.value, search = viewModel.showSearchBar.value)
                    }
                }
            }
        }
    }
}
