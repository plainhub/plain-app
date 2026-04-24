package com.ismartcoding.plain.ui.page.docs

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.preferences.DocSortByPreference
import com.ismartcoding.plain.ui.base.AnimatedBottomAction
import com.ismartcoding.plain.ui.base.MediaTopBar
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NeedPermissionColumn
import com.ismartcoding.plain.ui.base.NoDataColumn
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PScrollableTabRow
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.components.ListSearchBar
import com.ismartcoding.plain.ui.extensions.reset
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.DocsViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.exitSearchMode
import com.ismartcoding.plain.ui.page.audio.components.ViewAudioBottomSheet
import com.ismartcoding.plain.ui.page.tags.TagsBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DocsPage(
    navController: NavHostController,
    docsVM: DocsViewModel = viewModel(),
    tagsVM: TagsViewModel = viewModel(key = "docTagsVM"),
    castVM: CastViewModel = viewModel(key = "docsCastVM"),
    mediaFoldersVM: MediaFoldersViewModel = viewModel(key = "docFoldersVM"),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val docsState = DocsPageState.create(docsVM, tagsVM, mediaFoldersVM)
    val pagerState = docsState.pagerState
    val scrollBehavior = docsState.scrollBehavior
    val dragSelectState = docsState.dragSelectState
    val itemsState = docsState.itemsState
    val tagsState = docsState.tagsState
    val tagsMapState = docsState.tagsMapState
    val scrollState = docsState.scrollState

    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch {
            withIO { docsVM.loadAsync(context, tagsVM); mediaFoldersVM.loadAsync(context) }
            setRefreshState(RefreshContentState.Finished)
        }
    }

    BackHandler(enabled = dragSelectState.selectMode || docsVM.showSearchBar.value) {
        if (dragSelectState.selectMode) {
            dragSelectState.exitSelectMode()
        } else if (docsVM.showSearchBar.value && (!docsVM.searchActive.value || docsVM.queryText.value.isEmpty())) {
            docsVM.exitSearchMode()
            docsVM.showLoading.value = true
            scope.launch(Dispatchers.IO) { docsVM.loadAsync(context, tagsVM) }
        }
    }

    DocsPageEffects(docsState, docsVM, tagsVM, mediaFoldersVM)

    var isFirstPageEffect by remember { mutableStateOf(true) }
    var isFirstTabsModeEffect by remember { mutableStateOf(true) }

    LaunchedEffect(pagerState.currentPage) {
        if (isFirstPageEffect) {
            isFirstPageEffect = false
            return@LaunchedEffect
        }
        val tab = docsVM.tabs.value.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        if (docsVM.tabsShowTags.value) {
            when (tab.value) {
                "all" -> {
                    docsVM.trash.value = false
                    docsVM.tag.value = null
                }

                "trash" -> {
                    docsVM.trash.value = true
                    docsVM.tag.value = null
                }

                else -> {
                    docsVM.trash.value = false
                    docsVM.tag.value = tagsVM.itemsFlow.value.find { it.id == tab.value }
                }
            }
        } else {
            when (tab.value) {
                "" -> {
                    docsVM.trash.value = false
                    docsVM.fileType.value = ""
                }

                "trash" -> {
                    docsVM.trash.value = true
                    docsVM.fileType.value = ""
                }

                else -> {
                    docsVM.trash.value = false
                    docsVM.fileType.value = tab.value
                }
            }
        }
        scope.launch {
            scrollBehavior.reset()
            docsVM.scrollStateMap[pagerState.currentPage]?.scrollToItem(0)
                ?: scrollState.scrollToItem(0)
        }
        scope.launch(Dispatchers.IO) { docsVM.loadAsync(context, tagsVM) }
    }

    LaunchedEffect(docsVM.tabsShowTags.value) {
        if (isFirstTabsModeEffect) {
            isFirstTabsModeEffect = false
            return@LaunchedEffect
        }
        if (pagerState.currentPage != 0) {
            pagerState.scrollToPage(0)
        }
    }

    val docsTagsMap = remember(tagsMapState, tagsState) {
        tagsMapState.mapValues { entry ->
            entry.value.mapNotNull { relation -> tagsState.find { it.id == relation.tagId } }
        }
    }

    ViewDocBottomSheet(docsVM = docsVM, tagsVM = tagsVM, tagsMapState = tagsMapState, tagsState = tagsState, dragSelectState = dragSelectState)
    DocFoldersBottomSheet(docsVM, mediaFoldersVM, tagsVM)
    if (docsVM.showTagsDialog.value) {
        TagsBottomSheet(tagsVM) { docsVM.showTagsDialog.value = false }
    }

    PScaffold(
        topBar = {
            MediaTopBar(
                navController = navController,
                mediaVM = docsVM,
                tagsVM = tagsVM,
                castVM = castVM,
                dragSelectState = dragSelectState,
                bucketsMap = docsState.bucketsMap,
                itemsState = itemsState,
                scrollBehavior = scrollBehavior,
                scrollToTop = { scope.launch { docsVM.scrollStateMap[pagerState.currentPage]?.scrollToItem(0) } },
                defaultNavigationIcon = { NavigationBackIcon { navController.navigateUp() } },
                onSortSelected = { _, sortBy ->
                    scope.launch(Dispatchers.IO) {
                        DocSortByPreference.putAsync(context, sortBy)
                        docsVM.sortBy.value = sortBy
                        docsVM.loadAsync(context, tagsVM)
                    }
                },
                onSearchAction = { _, _ ->
                    scope.launch(Dispatchers.IO) {
                        docsVM.loadAsync(context, tagsVM)
                    }
                },
            )
        },
        bottomBar = {
            AnimatedBottomAction(visible = dragSelectState.showBottomActions()) {
                DocFilesSelectModeBottomActions(docsVM, tagsVM, tagsState, dragSelectState)
            }
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
            if (!docsVM.hasPermission.value) {
                NeedPermissionColumn(R.drawable.file_text, AppFeatureType.FILES.getPermission()!!)
                return@Column
            }
            if (!dragSelectState.selectMode) {
                PScrollableTabRow(selectedTabIndex = pagerState.currentPage, modifier = Modifier.fillMaxWidth()) {
                    docsVM.tabs.value.forEachIndexed { index, tab ->
                        PFilterChip(
                            modifier = Modifier.padding(start = if (index == 0) 0.dp else 8.dp),
                            selected = pagerState.currentPage == index,
                            onClick = { scope.launch { pagerState.scrollToPage(index) } },
                            label = { Text(text = "${tab.title} (${tab.count})") },
                        )
                    }
                }
            }
            DocsPageContent(
                navController = navController,
                docsVM = docsVM,
                tagsVM = tagsVM,
                itemsState = itemsState,
                dragSelectState = dragSelectState,
                docsTagsMap = docsTagsMap,
                pagerState = pagerState,
                scrollBehavior = scrollBehavior,
                topRefreshLayoutState = topRefreshLayoutState,
                paddingValues = paddingValues,
            )
        }
    }
}
