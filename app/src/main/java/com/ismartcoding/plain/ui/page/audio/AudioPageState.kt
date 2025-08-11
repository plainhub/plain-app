package com.ismartcoding.plain.ui.page.audio

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import com.ismartcoding.plain.data.DAudio
import com.ismartcoding.plain.data.DMediaBucket
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.db.DTagRelation
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.base.dragselect.rememberListDragSelectState
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel

@OptIn(ExperimentalMaterial3Api::class)
data class AudioPageState(
    val pagerState: PagerState,
    val itemsState: List<DAudio>,
    val dragSelectState: DragSelectState,
    val scrollBehavior: TopAppBarScrollBehavior,
    val tagsState: List<DTag>,
    val tagsMapState: Map<String, List<DTagRelation>>,
    val scrollState: LazyListState,
    val bucketsMap: Map<String, DMediaBucket>
) {
    companion object {
        @OptIn(ExperimentalMaterial3Api::class)
        @Composable
        fun create(
            audioVM: AudioViewModel,
            tagsVM: TagsViewModel,
            mediaFoldersVM: MediaFoldersViewModel,
        ): AudioPageState {
            LaunchedEffect(Unit) {
                tagsVM.dataType.value = audioVM.dataType
                mediaFoldersVM.dataType.value = audioVM.dataType
            }
            val tagsState by tagsVM.itemsFlow.collectAsState()
            val pagerState = rememberPagerState(pageCount = { 
                tagsState.size + if (AppFeatureType.MEDIA_TRASH.has()) 2 else 1 
            })
            val itemsState by audioVM.itemsFlow.collectAsState()
            val scrollState = rememberLazyListState()
            
            val dragSelectState = rememberListDragSelectState({ audioVM.scrollStateMap[pagerState.currentPage] })
            
            val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = {
                (audioVM.scrollStateMap[pagerState.currentPage]?.firstVisibleItemIndex ?: 0) > 0 && !dragSelectState.selectMode
            })
            
            val tagsMapState by tagsVM.tagsMapFlow.collectAsState()
            val bucketsMap by mediaFoldersVM.bucketsMapFlow.collectAsState()

            return AudioPageState(
                pagerState = pagerState,
                itemsState = itemsState,
                dragSelectState = dragSelectState,
                scrollBehavior = scrollBehavior,
                tagsState = tagsState,
                tagsMapState = tagsMapState,
                scrollState = scrollState,
                bucketsMap = bucketsMap
            )
        }
    }
} 