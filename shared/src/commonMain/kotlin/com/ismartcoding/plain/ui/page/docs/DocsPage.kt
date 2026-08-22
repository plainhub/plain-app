package com.ismartcoding.plain.ui.page.docs

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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.PBackHandler
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.preferences.DocSortByPreference
import com.ismartcoding.plain.ui.base.AnimatedBottomAction
import com.ismartcoding.plain.ui.base.MediaTopBar
import com.ismartcoding.plain.ui.base.NeedPermissionColumn
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PScrollableTabRow
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.extensions.reset
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.DocsViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.exitSearchMode
import com.ismartcoding.plain.ui.page.tags.TagsBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun DocsPage(
    navController: NavHostController,
    docsVM: DocsViewModel = viewModel { DocsViewModel() },
    tagsVM: TagsViewModel = viewModel(key = "docTagsVM") { TagsViewModel() },
    castVM: CastViewModel = viewModel(key = "docsCastVM") { CastViewModel() },
    mediaFoldersVM: MediaFoldersViewModel = viewModel(key = "docFoldersVM") { MediaFoldersViewModel() },
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

    DocsPageEffects(docsState, docsVM, tagsVM, mediaFoldersVM)

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
                        DocSortByPreference.putAsync(sortBy)
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
                NeedPermissionColumn(Res.drawable.file_text, AppFeatureType.FILES.getPermission()!!)
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
