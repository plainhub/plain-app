package com.ismartcoding.plain.ui.page.images

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.preferences.ImageSortByPreference
import com.ismartcoding.plain.ui.base.AnimatedBottomAction
import com.ismartcoding.plain.ui.base.MediaTopBar
import com.ismartcoding.plain.ui.base.NavigationBackIcon
import com.ismartcoding.plain.ui.base.NeedPermissionColumn
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.base.PScrollableTabRow
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewer
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VTabData
import com.ismartcoding.plain.ui.models.exitSearchMode
import com.ismartcoding.plain.ui.page.cast.CastDialog
import com.ismartcoding.plain.ui.page.home.MediaFoldersBottomSheet
import com.ismartcoding.plain.ui.page.tags.TagsBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImagesPage(
    navController: NavHostController,
    imagesVM: ImagesViewModel = viewModel(key = "imagesVM"),
    tagsVM: TagsViewModel = viewModel(key = "imageTagsVM"),
    mediaFoldersVM: MediaFoldersViewModel = viewModel(key = "imageFoldersVM"),
    castVM: CastViewModel = viewModel(key = "imageCastVM"),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val imagesState = ImagesPageState.create(imagesVM, tagsVM, mediaFoldersVM)
    val itemsState = imagesState.itemsState
    val configuration = LocalConfiguration.current
    val pagerState = imagesState.pagerState
    val scrollBehavior = imagesState.scrollBehavior
    val tagsState = imagesState.tagsState
    val previewerState = imagesState.previewerState
    val tagsMapState = imagesState.tagsMapState
    val dragSelectState = imagesState.dragSelectState
    val cellsPerRow = imagesState.cellsPerRow
    val isFirstTime = remember { mutableStateOf(true) }
    val density = LocalDensity.current
    val imageWidthPx = remember(cellsPerRow.value) { density.run { ((configuration.screenWidthDp.dp - ((cellsPerRow.value - 1) * 2).dp) / cellsPerRow.value).toPx().toInt() } }
    val tabs = remember(tagsState, imagesVM.total.intValue, imagesVM.totalTrash.intValue) {
        val baseTabs = mutableListOf(VTabData(LocaleHelper.getString(R.string.all), "all", imagesVM.total.intValue))
        if (AppFeatureType.MEDIA_TRASH.has()) baseTabs.add(VTabData(LocaleHelper.getString(R.string.trash), "trash", imagesVM.totalTrash.intValue))
        baseTabs.addAll(tagsState.map { VTabData(it.name, it.id, it.count) }); baseTabs
    }
    val topRefreshLayoutState = rememberRefreshLayoutState { scope.launch { withIO { imagesVM.loadAsync(context, tagsVM); mediaFoldersVM.loadAsync(context) }; setRefreshState(RefreshContentState.Finished) } }

    BackHandler(enabled = previewerState.visible || dragSelectState.selectMode || castVM.castMode.value || imagesVM.showSearchBar.value) {
        when {
            previewerState.visible -> scope.launch { previewerState.closeTransform() }
            dragSelectState.selectMode -> dragSelectState.exitSelectMode()
            castVM.castMode.value -> castVM.exitCastMode()
            imagesVM.showSearchBar.value && (!imagesVM.searchActive.value || imagesVM.queryText.value.isEmpty()) -> {
                imagesVM.exitSearchMode(); imagesVM.showLoading.value = true
                scope.launch(Dispatchers.IO) { imagesVM.loadAsync(context, tagsVM) }
            }
        }
    }

    ImagesPageEffects(context, imagesVM, tagsVM, mediaFoldersVM, scrollBehavior, pagerState, scope, dragSelectState, previewerState, cellsPerRow, isFirstTime, tabs, tagsState)

    ViewImageBottomSheet(imagesVM, tagsVM, tagsMapState, tagsState, dragSelectState)
    MediaFoldersBottomSheet(imagesVM, mediaFoldersVM, tagsVM)
    if (imagesVM.showTagsDialog.value) { TagsBottomSheet(tagsVM) { imagesVM.showTagsDialog.value = false } }
    CastDialog(castVM)

    PScaffold(
        topBar = {
            MediaTopBar(
                navController = navController,
                mediaVM = imagesVM,
                tagsVM = tagsVM,
                castVM = castVM,
                dragSelectState = dragSelectState,
                scrollBehavior = scrollBehavior,
                bucketsMap = imagesState.bucketsMap,
                itemsState = itemsState,
                scrollToTop = { scope.launch { imagesVM.scrollStateMap[pagerState.currentPage]?.scrollToItem(0) } },
                defaultNavigationIcon = { NavigationBackIcon { navController.popBackStack() } },
                onSortSelected = { _, sortBy ->
                    scope.launch(Dispatchers.IO) { ImageSortByPreference.putAsync(context, sortBy); imagesVM.sortBy.value = sortBy; imagesVM.loadAsync(context, tagsVM) }
                },
                onSearchAction = { ctx, tv -> scope.launch(Dispatchers.IO) { imagesVM.loadWithAiSearchAsync(ctx, tv) } },
            )
        },
        bottomBar = {
            AnimatedBottomAction(visible = dragSelectState.showBottomActions()) {
                ImageFilesSelectModeBottomActions(imagesVM, tagsVM, tagsState, dragSelectState)
            }
        },
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding())) {
            if (!imagesVM.hasPermission.value) { NeedPermissionColumn(R.drawable.image, AppFeatureType.FILES.getPermission()!!); return@PScaffold }
            if (!dragSelectState.selectMode) {
                PScrollableTabRow(selectedTabIndex = pagerState.currentPage, modifier = Modifier.fillMaxWidth()) {
                    tabs.forEachIndexed { index, s ->
                        PFilterChip(modifier = Modifier.padding(start = if (index == 0) 0.dp else 8.dp), selected = pagerState.currentPage == index, onClick = { scope.launch { pagerState.scrollToPage(index) } },
                            label = { if (index == 0) Text(text = s.title + " (" + s.count + ")") else Text(if (imagesVM.bucketId.value.isNotEmpty() || imagesVM.queryText.value.isNotEmpty()) s.title else "${s.title} (${s.count})") })
                    }
                }
            }
            ImagesPageGrid(context, imagesVM, tagsVM, castVM, itemsState, pagerState, scrollBehavior, topRefreshLayoutState, dragSelectState, previewerState, cellsPerRow, imageWidthPx, scope, paddingValues)
        }
    }

    MediaPreviewer(
        state = previewerState,
        tagsVM = tagsVM,
        tagsMap = tagsMapState,
        tagsState = tagsState,
        onRenamed = { scope.launch(Dispatchers.IO) { imagesVM.loadAsync(context, tagsVM) } },
        deleteAction = { item -> scope.launch(Dispatchers.IO) { imagesVM.delete(context, tagsVM, setOf(item.mediaId)); previewerState.closeTransform() } },
        onTagsChanged = {},
    )
}
