package com.ismartcoding.plain.ui.images

import com.ismartcoding.plain.ui.media.previewTrashActions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.ismartcoding.plain.models.Tag
import com.ismartcoding.plain.models.TagRelation
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.platform.media.MediaPreviewer
import com.ismartcoding.plain.ui.state.launchSafe
import kotlinx.coroutines.launch
import com.ismartcoding.plain.ui.tags.TagsViewModel

@Composable
internal fun ImagesPagePreviewer(
    imagesVM: ImagesViewModel,
    tagsVM: TagsViewModel,
    previewerState: MediaPreviewerState,
    tagsMapState: Map<String, List<TagRelation>>,
    tagsState: List<Tag>,
) {
    val scope = rememberCoroutineScope()
    MediaPreviewer(
        state = previewerState,
        tagsVM = tagsVM,
        tagsMap = tagsMapState,
        tagsState = tagsState,
        onRenamed = {
            scope.launchSafe(onDone = { imagesVM.showLoading.value = false }) {
                imagesVM.loadAsync(tagsVM)
            }
        },
        trashActions = imagesVM.previewTrashActions(tagsVM) {
            scope.launch { previewerState.closeTransform() }
        },
        onTagsChanged = {},
    )
}
