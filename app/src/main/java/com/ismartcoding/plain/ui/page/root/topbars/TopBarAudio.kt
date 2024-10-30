package com.ismartcoding.plain.ui.page.root.topbars

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.base.MediaTopBar
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.page.audio.AudioPageState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TopBarAudio(
    navController: NavHostController,
    audioState: AudioPageState,
    audioVM: AudioViewModel,
    tagsVM: TagsViewModel,
    castVM: CastViewModel,
) {
    val scope = rememberCoroutineScope()

    MediaTopBar(
        navController = navController,
        mediaVM = audioVM,
        tagsVM = tagsVM,
        castVM = castVM,
        dragSelectState = audioState.dragSelectState,
        scrollBehavior = audioState.scrollBehavior,
        bucketsMap = audioState.bucketsMap,
        itemsState = audioState.itemsState,
        scrollToTop = {
            scope.launch {
                audioVM.scrollStateMap[audioState.pagerState.currentPage]?.scrollToItem(0)
            }
        },
        showCellsPerRowDialog = false,
        onSearchAction = { context, tagsVM ->
            scope.launch(Dispatchers.IO) {
                audioVM.loadAsync(context, tagsVM)
            }
        }
    )
} 