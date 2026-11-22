package com.ismartcoding.plain.ui.images

import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import com.ismartcoding.plain.models.Tag
import com.ismartcoding.plain.models.TagRelation
import com.ismartcoding.plain.ui.media.viewer.previewer.MediaPreviewerState
import com.ismartcoding.plain.ui.platform.media.MediaPreviewer
import com.ismartcoding.plain.ui.state.launchSafe
import com.ismartcoding.plain.ui.tags.TagsViewModel
import kotlinx.coroutines.launch

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
        deleteAction = { item ->
            scope.launch {
                imagesVM.delete(tagsVM, setOf(item.mediaId))
                previewerState.closeTransform()
            }
        },
        onTagsChanged = {},
    )
}
