package com.ismartcoding.plain.ui.files.documents

import com.ismartcoding.plain.preferences.PrefKeys

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.ui.base.AnimatedBottomAction
import com.ismartcoding.plain.ui.components.NeedPermissionColumn
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.media.MediaTopBar
import com.ismartcoding.plain.ui.casting.CastViewModel
import com.ismartcoding.plain.ui.files.MediaFoldersViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.state.exitSearchMode
import com.ismartcoding.plain.ui.navigation.NavLoadGate
import com.ismartcoding.plain.ui.tags.TagsBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.file_text as ui_drawable_file_text

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DocsPage(
    navController: NavHostController,
    initialLoadGate: NavLoadGate? = null,
    source: com.ismartcoding.plain.rust.graphql.ContentSource = com.ismartcoding.plain.ui.navigation.LocalContentSource.current,
    docsVM: DocsViewModel = viewModel { DocsViewModel(source) },
    tagsVM: TagsViewModel = viewModel(key = "docTagsVM") { TagsViewModel(source.tags) },
    castVM: CastViewModel = viewModel(key = "docsCastVM") { CastViewModel() },
    mediaFoldersVM: MediaFoldersViewModel = viewModel(key = "docFoldersVM") { MediaFoldersViewModel(source) },
) {
    val scope = rememberCoroutineScope()
    val docsState = DocsPageState.create(docsVM, tagsVM, mediaFoldersVM)
    val scrollBehavior = docsState.scrollBehavior
    val dragSelectState = docsState.dragSelectState
    val itemsState = docsState.itemsState
    val tagsState = docsState.tagsState
    val tagsMapState = docsState.tagsMapState
    val scrollState = docsState.scrollState
    docsVM.scrollStateMap[0] = scrollState

    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch {
            docsVM.loadAsync(tagsVM)
            withIO { mediaFoldersVM.loadAsync() }
            setRefreshState(RefreshContentState.Finished)
        }
    }

    PBackHandler(enabled = dragSelectState.selectMode || docsVM.showSearchBar.value) {
        if (dragSelectState.selectMode) {
            dragSelectState.exitSelectMode()
        } else if (docsVM.showSearchBar.value && (!docsVM.searchActive.value || docsVM.queryText.value.isEmpty())) {
            docsVM.exitSearchMode()
            docsVM.showLoading.value = true
            scope.launch(Dispatchers.Default) { docsVM.loadAsync(tagsVM) }
        }
    }

    DocsPageEffects(docsState, docsVM, tagsVM, mediaFoldersVM, initialLoadGate)

    val docsTagsMap = remember(tagsMapState, tagsState) {
        tagsMapState.mapValues { entry ->
            entry.value.mapNotNull { relation -> tagsState.find { it.id == relation.tagId } }
        }
    }

    ViewDocBottomSheet(docsVM = docsVM, tagsVM = tagsVM, tagsMapState = tagsMapState, tagsState = tagsState, dragSelectState = dragSelectState)
    if (docsVM.showTagsDialog.value) {
        TagsBottomSheet(tagsVM) { docsVM.showTagsDialog.value = false }
    }

    MediaTopBar(
        navController = navController,
        mediaVM = docsVM,
        tagsVM = tagsVM,
        castVM = castVM,
        mediaFoldersVM = mediaFoldersVM,
        dragSelectState = dragSelectState,
                bucketsMap = docsState.bucketsMap,
                itemsState = itemsState,
                scrollBehavior = scrollBehavior,
                scrollToTop = { scope.launch { scrollState.scrollToItem(0) } },
                onSortSelected = { sortBy ->
                    scope.launch(Dispatchers.Default) {
                        Prefs.set(PrefKeys.DOC_SORT_BY, sortBy.name)
                        docsVM.sortBy.value = sortBy
                        docsVM.loadAsync(tagsVM)
                    }
                },
                onSearchAction = { _ ->
                    scope.launch(Dispatchers.Default) {
                        docsVM.loadAsync(tagsVM)
                    }
                },
        bottomBar = {
            AnimatedBottomAction(visible = dragSelectState.showBottomActions()) {
                DocFilesSelectModeBottomActions(docsVM, tagsVM, tagsState, dragSelectState)
            }
        },
    ) { paddingValues ->
        Column(modifier = Modifier.padding(top = paddingValues.calculateTopPadding())) {
            if (!docsVM.hasPermission.value) {
                NeedPermissionColumn(UiRes.drawable.ui_drawable_file_text, AppFeatureType.FILES.getPermission()!!)
                return@Column
            }

            DocsPageContent(
                navController = navController,
                docsVM = docsVM,
                tagsVM = tagsVM,
                itemsState = itemsState,
                dragSelectState = dragSelectState,
                docsTagsMap = docsTagsMap,
                scrollBehavior = scrollBehavior,
                topRefreshLayoutState = topRefreshLayoutState,
                paddingValues = paddingValues,
            )
        }
    }
}
