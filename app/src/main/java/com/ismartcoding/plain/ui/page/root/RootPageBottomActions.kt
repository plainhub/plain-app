package com.ismartcoding.plain.ui.page.root

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.ui.base.AnimatedBottomAction
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VideosViewModel
import com.ismartcoding.plain.ui.page.audio.components.AudioFilesSelectModeBottomActions
import com.ismartcoding.plain.ui.page.images.ImageFilesSelectModeBottomActions
import com.ismartcoding.plain.ui.page.videos.VideoFilesSelectModeBottomActions

@Composable
fun RootPageBottomActions(
    states: RootPageStates,
    imagesVM: ImagesViewModel,
    imageTagsVM: TagsViewModel,
    videosVM: VideosViewModel,
    videoTagsVM: TagsViewModel,
    audioVM: AudioViewModel,
    audioPlaylistVM: AudioPlaylistViewModel,
    audioTagsVM: TagsViewModel,
) {
    AnimatedBottomAction(visible = states.imagesState.dragSelectState.showBottomActions()) {
        ImageFilesSelectModeBottomActions(
            imagesVM,
            imageTagsVM,
            states.imagesState.tagsState,
            states.imagesState.dragSelectState,
        )
    }
    AnimatedBottomAction(visible = states.videosState.dragSelectState.showBottomActions()) {
        VideoFilesSelectModeBottomActions(
            videosVM,
            videoTagsVM,
            states.videosState.tagsState,
            states.videosState.dragSelectState,
        )
    }
    AnimatedBottomAction(visible = states.audioState.dragSelectState.showBottomActions()) {
        AudioFilesSelectModeBottomActions(
            audioVM,
            audioPlaylistVM,
            audioTagsVM,
            states.audioState.tagsState,
            states.audioState.dragSelectState,
        )
    }
}