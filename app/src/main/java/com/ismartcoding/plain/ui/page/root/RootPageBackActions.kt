package com.ismartcoding.plain.ui.page.root

import android.content.Context
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VideosViewModel
import com.ismartcoding.plain.ui.models.exitSearchMode
import com.ismartcoding.plain.ui.page.audio.AudioPageState
import com.ismartcoding.plain.ui.page.images.ImagesPageState
import com.ismartcoding.plain.ui.page.videos.VideosPageState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

fun handleImagesBack(
    scope: CoroutineScope,
    context: Context,
    imagesState: ImagesPageState,
    imagesVM: ImagesViewModel,
    imageTagsVM: TagsViewModel,
    imageCastVM: CastViewModel,
) {
    when {
        imagesState.previewerState.visible -> scope.launch {
            imagesState.previewerState.closeTransform()
        }

        imagesState.dragSelectState.selectMode -> imagesState.dragSelectState.exitSelectMode()

        imageCastVM.castMode.value -> imageCastVM.exitCastMode()

        imagesVM.showSearchBar.value && (!imagesVM.searchActive.value || imagesVM.queryText.value.isEmpty()) -> {
            imagesVM.exitSearchMode()
            imagesVM.showLoading.value = true
            scope.launch(Dispatchers.IO) {
                imagesVM.loadAsync(context, imageTagsVM)
            }
        }
    }
}

fun handleVideosBack(
    scope: CoroutineScope,
    context: Context,
    videosState: VideosPageState,
    videosVM: VideosViewModel,
    videoTagsVM: TagsViewModel,
    videoCastVM: CastViewModel,
) {
    when {
        videosState.previewerState.visible -> scope.launch {
            videosState.previewerState.closeTransform()
        }

        videosState.dragSelectState.selectMode -> videosState.dragSelectState.exitSelectMode()

        videoCastVM.castMode.value -> videoCastVM.exitCastMode()

        videosVM.showSearchBar.value && (!videosVM.searchActive.value || videosVM.queryText.value.isEmpty()) -> {
            videosVM.exitSearchMode()
            videosVM.showLoading.value = true
            scope.launch(Dispatchers.IO) {
                videosVM.loadAsync(context, videoTagsVM)
            }
        }
    }
}

fun handleAudioBack(
    scope: CoroutineScope,
    context: Context,
    audioState: AudioPageState,
    audioVM: AudioViewModel,
    audioTagsVM: TagsViewModel,
    audioCastVM: CastViewModel,
) {
    when {
        audioState.dragSelectState.selectMode -> audioState.dragSelectState.exitSelectMode()

        audioCastVM.castMode.value -> audioCastVM.exitCastMode()

        audioVM.showSearchBar.value && (!audioVM.searchActive.value || audioVM.queryText.value.isEmpty()) -> {
            audioVM.exitSearchMode()
            audioVM.showLoading.value = true
            scope.launch(Dispatchers.IO) {
                audioVM.loadAsync(context, audioTagsVM)
            }
        }
    }
}