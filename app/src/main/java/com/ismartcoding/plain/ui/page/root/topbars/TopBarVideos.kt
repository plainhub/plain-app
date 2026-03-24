package com.ismartcoding.plain.ui.page.root.topbars

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import com.ismartcoding.plain.preferences.VideoSortByPreference
import com.ismartcoding.plain.ui.base.ActionButtonDrawer
import com.ismartcoding.plain.ui.base.MediaTopBar
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VideosViewModel
import com.ismartcoding.plain.ui.page.videos.VideosPageState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TopBarVideos(
    navController: NavHostController,
    onOpenDrawer: () -> Unit,
    videosState: VideosPageState,
    videosVM: VideosViewModel,
    tagsVM: TagsViewModel,
    castVM: CastViewModel,
) {
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    MediaTopBar(
        navController = navController,
        mediaVM = videosVM,
        tagsVM = tagsVM,
        castVM = castVM,
        dragSelectState = videosState.dragSelectState,
        scrollBehavior = videosState.scrollBehavior,
        bucketsMap = videosState.bucketsMap,
        itemsState = videosState.itemsState,
        scrollToTop = {
            scope.launch {
                videosVM.scrollStateMap[videosState.pagerState.currentPage]?.scrollToItem(0)
            }
        },
        defaultNavigationIcon = {
            ActionButtonDrawer(onClick = onOpenDrawer)
        },
        onSortSelected = { _, sortBy ->
            scope.launch(Dispatchers.IO) {
                VideoSortByPreference.putAsync(context, sortBy)
                videosVM.sortBy.value = sortBy
                videosVM.loadAsync(context, tagsVM)
            }
        },
        onSearchAction = { context, tagsVM ->
            scope.launch(Dispatchers.IO) {
                videosVM.loadAsync(context, tagsVM)
            }
        }
    )
} 