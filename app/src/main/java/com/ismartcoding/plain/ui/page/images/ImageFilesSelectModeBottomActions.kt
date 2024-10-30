package com.ismartcoding.plain.ui.page.images

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.features.media.ImageMediaStoreHelper
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.components.MediaFilesSelectModeBottomActions

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageFilesSelectModeBottomActions(
    imagesVM: ImagesViewModel,
    tagsVM: TagsViewModel,
    tagsState: List<DTag>,
    dragSelectState: DragSelectState,
) {
    MediaFilesSelectModeBottomActions(
        viewModel = imagesVM,
        tagsVM = tagsVM,
        tagsState = tagsState,
        dragSelectState = dragSelectState,
        getItemUri = { ImageMediaStoreHelper.getItemUri(it) },
        getCollectableItems = { imagesVM.itemsFlow.collectAsStateValue() }
    )
}