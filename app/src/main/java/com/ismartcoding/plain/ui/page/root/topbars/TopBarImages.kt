package com.ismartcoding.plain.ui.page.root.topbars

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.base.MediaTopBar
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.page.images.ImagesPageState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun TopBarImages(
    navController: NavHostController,
    imagesState: ImagesPageState,
    imagesVM: ImagesViewModel,
    tagsVM: TagsViewModel,
    castVM: CastViewModel,
) {
    val scope = rememberCoroutineScope()

    MediaTopBar(
        navController = navController,
        mediaVM = imagesVM,
        tagsVM = tagsVM,
        castVM = castVM,
        dragSelectState = imagesState.dragSelectState,
        scrollBehavior = imagesState.scrollBehavior,
        bucketsMap = imagesState.bucketsMap,
        itemsState = imagesState.itemsState,
        scrollToTop = {
            scope.launch {
                imagesVM.scrollStateMap[imagesState.pagerState.currentPage]?.scrollToItem(0)
            }
        },
        onCellsPerRowClick = { imagesVM.showCellsPerRowDialog.value = true },
        onSearchAction = { context, tagsVM ->
            scope.launch(Dispatchers.IO) {
                imagesVM.loadAsync(context, tagsVM)
            }
        }
    )
}