package com.ismartcoding.plain.ui.page.root

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VideosViewModel
import com.ismartcoding.plain.ui.page.audio.AudioPageState
import com.ismartcoding.plain.ui.page.images.ImagesPageState
import com.ismartcoding.plain.ui.page.videos.VideosPageState

data class RootPageStates(
    val imagesState: ImagesPageState,
    val videosState: VideosPageState,
    val audioState: AudioPageState,
)

@Composable
fun rememberRootPageStates(
    imagesVM: ImagesViewModel,
    imageTagsVM: TagsViewModel,
    imageFoldersVM: MediaFoldersViewModel,
    videosVM: VideosViewModel,
    videoTagsVM: TagsViewModel,
    videoFoldersVM: MediaFoldersViewModel,
    audioVM: AudioViewModel,
    audioTagsVM: TagsViewModel,
    audioFoldersVM: MediaFoldersViewModel,
): RootPageStates {
    val imagesState = ImagesPageState.create(
        imagesVM = imagesVM,
        tagsVM = imageTagsVM,
        imageFoldersVM = imageFoldersVM,
    )

    val videosState = VideosPageState.create(
        videosVM = videosVM,
        tagsVM = videoTagsVM,
        mediaFoldersVM = videoFoldersVM,
    )

    val audioState = AudioPageState.create(
        audioVM = audioVM,
        tagsVM = audioTagsVM,
        mediaFoldersVM = audioFoldersVM,
    )

    return RootPageStates(
        imagesState = imagesState,
        videosState = videosState,
        audioState = audioState,
    )
}