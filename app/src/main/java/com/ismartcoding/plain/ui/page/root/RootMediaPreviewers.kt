package com.ismartcoding.plain.ui.page.root

import android.content.Context
import androidx.compose.runtime.Composable
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewer
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VideosViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun RootMediaPreviewers(
    states: RootPageStates,
    scope: CoroutineScope,
    context: Context,
    imagesVM: ImagesViewModel,
    imageTagsVM: TagsViewModel,
    videosVM: VideosViewModel,
    videoTagsVM: TagsViewModel,
) {
    val imagesState = states.imagesState
    val videosState = states.videosState

    MediaPreviewer(
        state = imagesState.previewerState,
        tagsVM = imageTagsVM,
        tagsMap = imagesState.tagsMapState,
        tagsState = imagesState.tagsState,
        onRenamed = {
            scope.launch(Dispatchers.IO) {
                imagesVM.loadAsync(context, imageTagsVM)
            }
        },
        deleteAction = { item ->
            scope.launch(Dispatchers.IO) {
                imagesVM.delete(context, imageTagsVM, setOf(item.mediaId))
                imagesState.previewerState.closeTransform()
            }
        },
        onTagsChanged = {},
    )

    MediaPreviewer(
        state = videosState.previewerState,
        tagsVM = videoTagsVM,
        tagsMap = videosState.tagsMapState,
        tagsState = videosState.tagsState,
        onRenamed = {
            scope.launch(Dispatchers.IO) {
                videosVM.loadAsync(context, videoTagsVM)
            }
        },
        deleteAction = { item ->
            scope.launch(Dispatchers.IO) {
                videosVM.delete(context, videoTagsVM, setOf(item.mediaId))
                videosState.previewerState.closeTransform()
            }
        },
        onTagsChanged = {},
    )
}