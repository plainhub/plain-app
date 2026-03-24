package com.ismartcoding.plain.ui.page.root

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VideosViewModel
import com.ismartcoding.plain.ui.page.root.components.RootPageType
import com.ismartcoding.plain.ui.page.root.topbars.TopBarAudio
import com.ismartcoding.plain.ui.page.root.topbars.TopBarChat
import com.ismartcoding.plain.ui.page.root.topbars.TopBarHome
import com.ismartcoding.plain.ui.page.root.topbars.TopBarImages
import com.ismartcoding.plain.ui.page.root.topbars.TopBarVideos

@Composable
fun RootPageTopBar(
    navController: NavHostController,
    currentPage: Int,
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
    onOpenDrawer: () -> Unit,
) {
    when (currentPage) {
        RootPageType.HOME.value -> TopBarHome(
            navController = navController,
            onOpenDrawer = onOpenDrawer,
        )
        RootPageType.IMAGES.value -> TopBarImages(
            navController = navController,
            onOpenDrawer = onOpenDrawer,
            imagesState = states.imagesState,
            imagesVM = imagesVM,
            tagsVM = imageTagsVM,
            castVM = imageCastVM,
        )

        RootPageType.AUDIO.value -> TopBarAudio(
            navController = navController,
            onOpenDrawer = onOpenDrawer,
            audioState = states.audioState,
            audioVM = audioVM,
            tagsVM = audioTagsVM,
            castVM = audioCastVM,
        )

        RootPageType.VIDEOS.value -> TopBarVideos(
            navController = navController,
            onOpenDrawer = onOpenDrawer,
            videosState = states.videosState,
            videosVM = videosVM,
            tagsVM = videoTagsVM,
            castVM = videoCastVM,
        )

        RootPageType.CHAT.value -> TopBarChat(
            navController = navController,
            channelVM = channelVM,
            onOpenDrawer = onOpenDrawer,
        )
    }
}
