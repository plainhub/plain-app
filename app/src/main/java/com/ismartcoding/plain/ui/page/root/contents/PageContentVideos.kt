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
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VTabData
import com.ismartcoding.plain.ui.models.VideosViewModel
import com.ismartcoding.plain.ui.page.root.MediaFoldersBottomSheet
import com.ismartcoding.plain.ui.page.tags.TagsBottomSheet
import com.ismartcoding.plain.ui.page.videos.VideosPageState
import com.ismartcoding.plain.ui.page.videos.ViewVideoBottomSheet
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PageContentVideos(videosState: VideosPageState, videosVM: VideosViewModel, tagsVM: TagsViewModel, mediaFoldersVM: MediaFoldersViewModel, castVM: CastViewModel, paddingValues: PaddingValues) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val itemsState = videosState.itemsState
    val configuration = LocalConfiguration.current
    val pagerState = videosState.pagerState
    val scrollBehavior = videosState.scrollBehavior
    val tagsState = videosState.tagsState
    val previewerState = videosState.previewerState
    val tagsMapState = videosState.tagsMapState
    val dragSelectState = videosState.dragSelectState
    val cellsPerRow = videosState.cellsPerRow
    val isFirstTime = remember { mutableStateOf(true) }
    val density = LocalDensity.current
    val imageWidthPx = remember(cellsPerRow.value) { density.run { ((configuration.screenWidthDp.dp - ((cellsPerRow.value - 1) * 2).dp) / cellsPerRow.value).toPx().toInt() } }
    val tabs = remember(tagsState, videosVM.total.intValue, videosVM.totalTrash.intValue) {
        val baseTabs = mutableListOf(VTabData(LocaleHelper.getString(R.string.all), "all", videosVM.total.intValue))
        if (AppFeatureType.MEDIA_TRASH.has()) baseTabs.add(VTabData(LocaleHelper.getString(R.string.trash), "trash", videosVM.totalTrash.intValue))
        baseTabs.addAll(tagsState.map { VTabData(it.name, it.id, it.count) }); baseTabs
    }
    val topRefreshLayoutState = rememberRefreshLayoutState { scope.launch { withIO { videosVM.loadAsync(context, tagsVM); mediaFoldersVM.loadAsync(context) }; setRefreshState(RefreshContentState.Finished) } }

    PageContentVideosEffects(context, videosVM, tagsVM, mediaFoldersVM, scrollBehavior, pagerState, scope, dragSelectState, previewerState, cellsPerRow, isFirstTime, tabs, tagsState)

    ViewVideoBottomSheet(videosVM, tagsVM, tagsMapState, tagsState, dragSelectState)
    MediaFoldersBottomSheet(videosVM, mediaFoldersVM, tagsVM)
    if (videosVM.showTagsDialog.value) { TagsBottomSheet(tagsVM) { videosVM.showTagsDialog.value = false } }
    CastDialog(castVM)

    Column(modifier = Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding())) {
        if (!videosVM.hasPermission.value) { NeedPermissionColumn(R.drawable.video, AppFeatureType.FILES.getPermission()!!); return }
        if (!dragSelectState.selectMode) {
            PScrollableTabRow(selectedTabIndex = pagerState.currentPage, modifier = Modifier.fillMaxWidth()) {
                tabs.forEachIndexed { index, s ->
                    PFilterChip(modifier = Modifier.padding(start = if (index == 0) 0.dp else 8.dp), selected = pagerState.currentPage == index, onClick = { scope.launch { pagerState.scrollToPage(index) } },
                        label = { if (index == 0) Text(text = s.title + " (" + s.count + ")") else Text(if (videosVM.bucketId.value.isNotEmpty() || videosVM.queryText.value.isNotEmpty()) s.title else "${s.title} (${s.count})") })
                }
            }
        }
        PageContentVideosGrid(context, videosVM, tagsVM, castVM, itemsState, pagerState, scrollBehavior, topRefreshLayoutState, dragSelectState, previewerState, cellsPerRow, imageWidthPx, scope, paddingValues)
    }
}
