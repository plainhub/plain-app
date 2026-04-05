package com.ismartcoding.plain.ui.page.images

import android.content.Context
import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import com.ismartcoding.lib.channel.Channel
import com.ismartcoding.lib.extensions.isGestureInteractionMode
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.preferences.ImageGridCellsPerRowPreference
import com.ismartcoding.plain.preferences.ImageSortByPreference
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.extensions.reset
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VTabData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImagesPageEffects(
    context: Context, imagesVM: ImagesViewModel, tagsVM: TagsViewModel,
    mediaFoldersVM: MediaFoldersViewModel, scrollBehavior: TopAppBarScrollBehavior,
    pagerState: PagerState, scope: CoroutineScope,
    dragSelectState: DragSelectState, previewerState: MediaPreviewerState,
    cellsPerRow: MutableState<Int>, isFirstTime: MutableState<Boolean>,
    tabs: List<VTabData>, tagsState: List<DTag>,
) {
    LaunchedEffect(Unit) {
        imagesVM.hasPermission.value = AppFeatureType.FILES.hasPermission(context)
        if (imagesVM.hasPermission.value) {
            scope.launch(Dispatchers.IO) {
                cellsPerRow.value = ImageGridCellsPerRowPreference.getAsync(context)
                imagesVM.sortBy.value = ImageSortByPreference.getValueAsync(context)
                imagesVM.loadAsync(context, tagsVM); mediaFoldersVM.loadAsync(context)
            }
        }
    }
    LaunchedEffect(Channel.sharedFlow) {
        Channel.sharedFlow.collect { event ->
            when (event) {
                is PermissionsResultEvent -> {
                    imagesVM.hasPermission.value = AppFeatureType.FILES.hasPermission(context)
                    scope.launch(Dispatchers.IO) { imagesVM.sortBy.value = ImageSortByPreference.getValueAsync(context); imagesVM.loadAsync(context, tagsVM) }
                }
            }
        }
    }
    LaunchedEffect(dragSelectState.selectMode, (previewerState.visible && !context.isGestureInteractionMode())) {
        if (dragSelectState.selectMode || (previewerState.visible && !context.isGestureInteractionMode())) scrollBehavior.reset()
    }
    LaunchedEffect(pagerState.currentPage) {
        if (isFirstTime.value) { isFirstTime.value = false; return@LaunchedEffect }
        val tab = tabs.getOrNull(pagerState.currentPage) ?: return@LaunchedEffect
        when (tab.value) {
            "all" -> { imagesVM.trash.value = false; imagesVM.tag.value = null }
            "trash" -> { imagesVM.trash.value = true; imagesVM.tag.value = null }
            else -> { imagesVM.trash.value = false; imagesVM.tag.value = tagsState.find { it.id == tab.value } }
        }
        scope.launch { scrollBehavior.reset(); imagesVM.scrollStateMap[pagerState.currentPage]?.scrollToItem(0) }
        scope.launch(Dispatchers.IO) { imagesVM.loadAsync(context, tagsVM) }
    }
}
