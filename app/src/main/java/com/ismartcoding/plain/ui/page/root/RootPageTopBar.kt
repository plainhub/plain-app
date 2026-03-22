package com.ismartcoding.plain.ui.page.root

import androidx.compose.runtime.Composable
import androidx.compose.foundation.pager.PagerState
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VideosViewModel
import com.ismartcoding.plain.ui.page.root.components.RootTabType
import com.ismartcoding.plain.ui.page.root.topbars.TopBarAudio
import com.ismartcoding.plain.ui.page.root.topbars.TopBarChat
import com.ismartcoding.plain.ui.page.root.topbars.TopBarHome
import com.ismartcoding.plain.ui.page.root.topbars.TopBarImages
import com.ismartcoding.plain.ui.page.root.topbars.TopBarVideos

@Composable
fun RootPageTopBar(
    navController: NavHostController,
    pagerState: PagerState,
    states: RootPageStates,
    imagesVM: ImagesViewModel,
    imageTagsVM: TagsViewModel,
    imageCastVM: CastViewModel,
    videosVM: VideosViewModel,
    videoTagsVM: TagsViewModel,
    videoCastVM: CastViewModel,
    audioVM: AudioViewModel,
    audioTagsVM: TagsViewModel,
    audioCastVM: CastViewModel,
    channelVM: ChannelViewModel,
) {
    when (pagerState.currentPage) {
        RootTabType.HOME.value -> TopBarHome(navController = navController)
        RootTabType.IMAGES.value -> TopBarImages(
            navController = navController,
            imagesState = states.imagesState,
            imagesVM = imagesVM,
            tagsVM = imageTagsVM,
            castVM = imageCastVM,
        )

        RootTabType.AUDIO.value -> TopBarAudio(
            navController = navController,
            audioState = states.audioState,
            audioVM = audioVM,
            tagsVM = audioTagsVM,
            castVM = audioCastVM,
        )

        RootTabType.VIDEOS.value -> TopBarVideos(
            navController = navController,
            videosState = states.videosState,
            videosVM = videosVM,
            tagsVM = videoTagsVM,
            castVM = videoCastVM,
        )

        RootTabType.CHAT.value -> TopBarChat(
            navController = navController,
            channelVM = channelVM,
        )
    }
}