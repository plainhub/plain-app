package com.ismartcoding.plain.ui.page.root.contents

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.base.NeedPermissionColumn
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PScrollableTabRow
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.page.cast.CastDialog
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VTabData
import com.ismartcoding.plain.ui.page.root.MediaFoldersBottomSheet
import com.ismartcoding.plain.ui.page.images.ImagesPageState
import com.ismartcoding.plain.ui.page.images.ViewImageBottomSheet
import com.ismartcoding.plain.ui.page.tags.TagsBottomSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageContentImages(imagesState: ImagesPageState, imagesVM: ImagesViewModel, tagsVM: TagsViewModel, mediaFoldersVM: MediaFoldersViewModel, castVM: CastViewModel, paddingValues: PaddingValues) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
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

    PageContentImagesEffects(context, imagesVM, tagsVM, mediaFoldersVM, scrollBehavior, pagerState, scope, dragSelectState, previewerState, cellsPerRow, isFirstTime, tabs, tagsState)

    ViewImageBottomSheet(imagesVM, tagsVM, tagsMapState, tagsState, dragSelectState)
    MediaFoldersBottomSheet(imagesVM, mediaFoldersVM, tagsVM)
    if (imagesVM.showTagsDialog.value) { TagsBottomSheet(tagsVM) { imagesVM.showTagsDialog.value = false } }
    CastDialog(castVM)

    Column(modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding())) {
        if (!imagesVM.hasPermission.value) { NeedPermissionColumn(R.drawable.image, AppFeatureType.FILES.getPermission()!!); return }
        if (!dragSelectState.selectMode) {
            PScrollableTabRow(selectedTabIndex = pagerState.currentPage, modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, s ->
                    PFilterChip(modifier = Modifier.padding(start = if (index == 0) 0.dp else 8.dp), selected = pagerState.currentPage == index, onClick = { scope.launch { pagerState.scrollToPage(index) } },
                        label = { if (index == 0) Text(text = s.title + " (" + s.count + ")") else Text(if (imagesVM.bucketId.value.isNotEmpty() || imagesVM.queryText.value.isNotEmpty()) s.title else "${s.title} (${s.count})") })
                }
            }
        }
        PageContentImagesGrid(context, imagesVM, tagsVM, castVM, itemsState, pagerState, scrollBehavior, topRefreshLayoutState, dragSelectState, previewerState, cellsPerRow, imageWidthPx, scope, paddingValues)
    }
}
