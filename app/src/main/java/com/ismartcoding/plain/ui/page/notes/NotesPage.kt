package com.ismartcoding.plain.ui.page.notes

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.ActionButtonSearch
import com.ismartcoding.plain.ui.base.ActionButtonTags
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NavigationCloseIcon
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PDraggableElement
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PScrollableTabRow
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.PTopRightButton
import com.ismartcoding.plain.ui.base.TopSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.fastscroll.LazyColumnScrollbar
import com.ismartcoding.plain.ui.base.pullrefresh.LoadMoreRefreshContent
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.ListSearchBar
import com.ismartcoding.plain.ui.components.NoteListItem
import com.ismartcoding.plain.ui.extensions.reset
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VTabData
import com.ismartcoding.plain.ui.models.enterSearchMode
import com.ismartcoding.plain.ui.models.exitSearchMode
import com.ismartcoding.plain.ui.models.exitSelectMode
import com.ismartcoding.plain.ui.models.isAllSelected
import com.ismartcoding.plain.ui.models.select
import com.ismartcoding.plain.ui.models.showBottomActions
import com.ismartcoding.plain.ui.models.toggleSelectAll
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.tags.TagsBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NotesPage(
    navController: NavHostController,
    notesVM: NotesViewModel,
    tagsVM: TagsViewModel,
) {
    val itemsState by notesVM.itemsFlow.collectAsState()
    val tagsState by tagsVM.itemsFlow.collectAsState()
    val tagsMapState by tagsVM.tagsMapFlow.collectAsState()
    val scope = rememberCoroutineScope()
    val scrollStateMap = remember {
        mutableStateMapOf<Int, LazyListState>()
    }
    val pagerState = rememberPagerState(pageCount = { tagsState.size + 2 })
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = {
        (scrollStateMap[pagerState.currentPage]?.firstVisibleItemIndex ?: 0) > 0 && !notesVM.selectMode.value
    })
    var isFirstTime by remember { mutableStateOf(true) }
    val tabs = remember(tagsState, notesVM.total.intValue, notesVM.totalTrash.intValue) {
        listOf(
            VTabData(LocaleHelper.getString(R.string.all), "all", notesVM.total.intValue),
            VTabData(LocaleHelper.getString(R.string.trash), "trash", notesVM.totalTrash.intValue),
            *tagsState.map { VTabData(it.name, it.id, it.count) }.toTypedArray()
        )
    }

    val topRefreshLayoutState =
        rememberRefreshLayoutState {
            scope.launch {
                withIO {
                    notesVM.loadAsync(tagsVM)
                }
                setRefreshState(RefreshContentState.Finished)
            }
        }

    LaunchedEffect(Unit) {
        tagsVM.dataType.value = notesVM.dataType
        scope.launch(Dispatchers.IO) {
            notesVM.loadAsync(tagsVM)
        }
    }

    LaunchedEffect(notesVM.selectMode.value) {
        if (notesVM.selectMode.value) {
            scrollBehavior.reset()
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        if (isFirstTime) {
            isFirstTime = false
            return@LaunchedEffect
        }
        when (val index = pagerState.currentPage) {
            0 -> {
                notesVM.trash.value = false
                notesVM.tag.value = null
            }

            1 -> {
                notesVM.trash.value = true
                notesVM.tag.value = null
            }

            else -> {
                notesVM.trash.value = false
                notesVM.tag.value = tagsState.getOrNull(index - 2)
            }
        }
        scope.launch {
            scrollBehavior.reset()
            scrollStateMap[pagerState.currentPage]?.scrollToItem(0)
        }
        scope.launch(Dispatchers.IO) {
            notesVM.loadAsync(tagsVM)
        }
    }

    val pageTitle = if (notesVM.selectMode.value) {
        LocaleHelper.getStringF(R.string.x_selected, "count", notesVM.selectedIds.size)
    } else if (notesVM.tag.value != null) {
        stringResource(id = R.string.notes) + " - " + notesVM.tag.value!!.name
    } else if (notesVM.trash.value) {
        stringResource(id = R.string.notes) + " - " + stringResource(id = R.string.trash)
    } else {
        stringResource(id = R.string.notes)
    }

    ViewNoteBottomSheet(
        notesVM,
        tagsVM,
        tagsMapState,
        tagsState,
    )

    if (notesVM.showTagsDialog.value) {
        TagsBottomSheet(tagsVM) {
            notesVM.showTagsDialog.value = false
        }
    }

    val onSearch: (String) -> Unit = {
        notesVM.searchActive.value = false
        notesVM.showLoading.value = true
        scope.launch {
            scrollStateMap[pagerState.currentPage]?.scrollToItem(0)
        }
        scope.launch(Dispatchers.IO) {
            notesVM.loadAsync(tagsVM)
        }
    }

    BackHandler(enabled = notesVM.selectMode.value || notesVM.showSearchBar.value) {
        if (notesVM.selectMode.value) {
            notesVM.exitSelectMode()
        } else if (notesVM.showSearchBar.value) {
            if (!notesVM.searchActive.value || notesVM.queryText.value.isEmpty()) {
                notesVM.exitSearchMode()
                onSearch("")
            }
        }
    }

    PScaffold(
        topBar = {
            if (notesVM.showSearchBar.value) {
                ListSearchBar(
                    viewModel = notesVM,
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
                    if (notesVM.selectMode.value) {
                        NavigationCloseIcon {
                            notesVM.exitSelectMode()
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
                    if (notesVM.selectMode.value) {
                        PTopRightButton(
                            label = stringResource(if (notesVM.isAllSelected()) R.string.unselect_all else R.string.select_all),
                            click = {
                                notesVM.toggleSelectAll()
                            },
                        )
                        HorizontalSpace(dp = 8.dp)
                    } else {
                        ActionButtonSearch {
                            notesVM.enterSearchMode()
                        }
                        ActionButtonTags {
                            notesVM.showTagsDialog.value = true
                        }
                    }
                },
            )
        },
        bottomBar = {
            AnimatedVisibility(
                visible = notesVM.showBottomActions(),
                enter = slideInVertically { it },
                exit = slideOutVertically { it }) {
                NotesSelectModeBottomActions(notesVM, tagsVM, tagsState)
            }
        },
        floatingActionButton = if (notesVM.selectMode.value) null else {
            {
                PDraggableElement {
                    FloatingActionButton(
                        onClick = {
                            navController.navigate(Routing.NotesCreate(notesVM.tag.value?.id ?: ""))
                        },
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.plus),
                            stringResource(R.string.add),
                        )
                    }
                }
            }
        },
    ) { paddingValues ->
        Column(Modifier.padding(top = paddingValues.calculateTopPadding())) {

            if (!notesVM.selectMode.value) {
                PScrollableTabRow(
                    selectedTabIndex = pagerState.currentPage,
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    tabs.forEachIndexed { index, s ->
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
                                    Text(if (notesVM.queryText.value.isNotEmpty()) s.title else "${s.title} (${s.count})")
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
                                            notesVM,
                                            m,
                                            tagsState.filter { tagIds.contains(it.id) },
                                            onClick = {
                                                if (notesVM.selectMode.value) {
                                                    notesVM.select(m.id)
                                                } else {
                                                    navController.navigate(Routing.NoteDetail(m.id))
                                                }
                                            },
                                            onLongClick = {
                                                if (notesVM.selectMode.value) {
                                                    return@NoteListItem
                                                }
                                                notesVM.selectedItem.value = m
                                            },
                                            onClickTag = { tag ->
                                                if (notesVM.selectMode.value) {
                                                    return@NoteListItem
                                                }
                                                val idx = tabs.indexOfFirst { it.value == tag.id }
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
                                        if (itemsState.isNotEmpty() && !notesVM.noMore.value) {
                                            LaunchedEffect(Unit) {
                                                scope.launch(Dispatchers.IO) {
                                                    withIO { notesVM.moreAsync(tagsVM) }
                                                }
                                            }
                                        }
                                        LoadMoreRefreshContent(notesVM.noMore.value)
                                    }
                                    item {
                                        VerticalSpace(dp = paddingValues.calculateBottomPadding())
                                    }
                                }
                            }
                        } else {
                            NoDataColumn(loading = notesVM.showLoading.value, search = notesVM.showSearchBar.value)
                        }
                    }
                }
            }
        }
    }
}
