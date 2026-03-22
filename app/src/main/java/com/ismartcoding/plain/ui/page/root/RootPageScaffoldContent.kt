package com.ismartcoding.plain.ui.page.root

import androidx.compose.foundation.pager.PagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VideosViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootPageScaffoldContent(
    navController: NavHostController,
    pagerState: PagerState,
    states: RootPageStates,
    mainVM: MainViewModel,
    imagesVM: ImagesViewModel,
    imageTagsVM: TagsViewModel,
    imageFoldersVM: MediaFoldersViewModel,
    imageCastVM: CastViewModel,
    videosVM: VideosViewModel,
    videoTagsVM: TagsViewModel,
    videoFoldersVM: MediaFoldersViewModel,
    videoCastVM: CastViewModel,
    audioVM: AudioViewModel,
    audioTagsVM: TagsViewModel,
    audioPlaylistVM: AudioPlaylistViewModel,
    audioFoldersVM: MediaFoldersViewModel,
    audioCastVM: CastViewModel,
    peerVM: PeerViewModel,
    channelVM: ChannelViewModel,
    onTabSelected: (Int) -> Unit,
) {
    PScaffold(
        topBar = {
            RootPageTopBar(
                navController = navController,
                pagerState = pagerState,
                states = states,
                imagesVM = imagesVM,
                imageTagsVM = imageTagsVM,
                imageCastVM = imageCastVM,
                videosVM = videosVM,
                videoTagsVM = videoTagsVM,
                videoCastVM = videoCastVM,
                audioVM = audioVM,
                audioTagsVM = audioTagsVM,
                audioCastVM = audioCastVM,
                channelVM = channelVM,
            )
        },
        bottomBar = {
            RootPageBottomActions(
                states = states,
                imagesVM = imagesVM,
                imageTagsVM = imageTagsVM,
                videosVM = videosVM,
                videoTagsVM = videoTagsVM,
                audioVM = audioVM,
                audioPlaylistVM = audioPlaylistVM,
                audioTagsVM = audioTagsVM,
            )
        },
    ) { paddingValues ->
        RootPagePagerContent(
            navController = navController,
            pagerState = pagerState,
            paddingValues = paddingValues,
            states = states,
            mainVM = mainVM,
            imagesVM = imagesVM,
            imageTagsVM = imageTagsVM,
            imageFoldersVM = imageFoldersVM,
            imageCastVM = imageCastVM,
            videosVM = videosVM,
            videoTagsVM = videoTagsVM,
            videoFoldersVM = videoFoldersVM,
            videoCastVM = videoCastVM,
            audioVM = audioVM,
            audioTagsVM = audioTagsVM,
            audioPlaylistVM = audioPlaylistVM,
            audioFoldersVM = audioFoldersVM,
            audioCastVM = audioCastVM,
            peerVM = peerVM,
            channelVM = channelVM,
            onTabSelected = onTabSelected,
        )
    }
}