package com.ismartcoding.plain.ui.page.videos

import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.runtime.Composable
import com.ismartcoding.plain.db.DTag
import com.ismartcoding.plain.features.media.VideoMediaStoreHelper
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.extensions.collectAsStateValue
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VideosViewModel
import com.ismartcoding.plain.ui.components.MediaFilesSelectModeBottomActions

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun VideoFilesSelectModeBottomActions(
    videosVM: VideosViewModel,
    tagsVM: TagsViewModel,
    tagsState: List<DTag>,
    dragSelectState: DragSelectState,
) {
    MediaFilesSelectModeBottomActions(
        vm = videosVM,
        tagsVM = tagsVM,
        tagsState = tagsState,
        dragSelectState = dragSelectState,
        getItemUri = { VideoMediaStoreHelper.getItemUri(it) },
        getCollectableItems = { videosVM.itemsFlow.collectAsStateValue() },
        isInTrashMode = videosVM.trash.value
    )
}