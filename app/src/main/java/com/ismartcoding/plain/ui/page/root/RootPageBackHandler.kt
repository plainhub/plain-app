package com.ismartcoding.plain.ui.page.root

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VideosViewModel
import com.ismartcoding.plain.ui.page.root.components.RootTabType
import kotlinx.coroutines.CoroutineScope

@Composable
fun RootPageBackHandler(
    pagerState: PagerState,
    states: RootPageStates,
    context: Context,
    scope: CoroutineScope,
    imagesVM: ImagesViewModel,
    imageTagsVM: TagsViewModel,
    imageCastVM: CastViewModel,
    videosVM: VideosViewModel,
    videoTagsVM: TagsViewModel,
    videoCastVM: CastViewModel,
    audioVM: AudioViewModel,
    audioTagsVM: TagsViewModel,
    audioCastVM: CastViewModel,
) {
    val imagesState = states.imagesState
    val videosState = states.videosState
    val audioState = states.audioState
    BackHandler(enabled = when (pagerState.currentPage) {
        RootTabType.IMAGES.value -> imagesState.previewerState.visible ||
            imagesState.dragSelectState.selectMode ||
            imageCastVM.castMode.value ||
            imagesVM.showSearchBar.value

        RootTabType.VIDEOS.value -> videosState.previewerState.visible ||
            videosState.dragSelectState.selectMode ||
            videoCastVM.castMode.value ||
            videosVM.showSearchBar.value

        RootTabType.AUDIO.value -> audioState.dragSelectState.selectMode ||
            audioCastVM.castMode.value ||
            audioVM.showSearchBar.value

        else -> false
    }) {
        when (pagerState.currentPage) {
            RootTabType.IMAGES.value -> handleImagesBack(
                scope,
                context,
                imagesState,
                imagesVM,
                imageTagsVM,
                imageCastVM,
            )

            RootTabType.VIDEOS.value -> handleVideosBack(
                scope,
                context,
                videosState,
                videosVM,
                videoTagsVM,
                videoCastVM,
            )

            RootTabType.AUDIO.value -> handleAudioBack(
                scope,
                context,
                audioState,
                audioVM,
                audioTagsVM,
                audioCastVM,
            )
        }
    }
}