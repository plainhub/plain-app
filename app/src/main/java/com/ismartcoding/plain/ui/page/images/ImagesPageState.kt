package com.ismartcoding.plain.ui.page.images

import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import com.ismartcoding.plain.data.DImage
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.preferences.ImageGridCellsPerRowPreference
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.base.dragselect.rememberDragSelectState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.rememberPreviewerState
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel

@OptIn(ExperimentalMaterial3Api::class)
data class ImagesPageState(
    val pagerState: PagerState,
    val itemsState: List<DImage>,
    val dragSelectState: DragSelectState,
    val scrollBehavior: TopAppBarScrollBehavior,
    val previewerState: MediaPreviewerState,
    val tagsState: List<DTag>,
    val tagsMapState: Map<String, List<DTagRelation>>,
    val bucketsMap: Map<String, DMediaBucket>,
    val cellsPerRow: MutableState<Int>
) {
    companion object {
        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        fun create(
            imagesVM: ImagesViewModel,
            tagsVM: TagsViewModel,
            imageFoldersVM: MediaFoldersViewModel,
        ): ImagesPageState {
            LaunchedEffect(Unit) {
                tagsVM.dataType.value = imagesVM.dataType
                imageFoldersVM.dataType.value = imagesVM.dataType
            }
            val tagsState by tagsVM.itemsFlow.collectAsState()
            val pagerState = rememberPagerState(pageCount = { 
                tagsState.size + if (AppFeatureType.MEDIA_TRASH.has()) 2 else 1 
            })
            val itemsState by imagesVM.itemsFlow.collectAsState()
            val dragSelectState = rememberDragSelectState({ imagesVM.scrollStateMap[pagerState.currentPage] })
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = {
                (imagesVM.scrollStateMap[pagerState.currentPage]?.firstVisibleItemIndex ?: 0) > 0 && !dragSelectState.selectMode
            })
            val previewerState = rememberPreviewerState()
            val tagsMapState by tagsVM.tagsMapFlow.collectAsState()
            val bucketsMap by imageFoldersVM.bucketsMapFlow.collectAsState()
            val cellsPerRow = remember { mutableIntStateOf(ImageGridCellsPerRowPreference.default) }

            return ImagesPageState(
                pagerState = pagerState,
                itemsState = itemsState,
                dragSelectState = dragSelectState,
                scrollBehavior = scrollBehavior,
                previewerState = previewerState,
                tagsState = tagsState,
                tagsMapState = tagsMapState,
                bucketsMap = bucketsMap,
                cellsPerRow = cellsPerRow
            )
        }
    }
}