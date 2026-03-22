package com.ismartcoding.plain.ui.page.root

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.PaddingValues
import androidx.navigation.NavHostController
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
import com.ismartcoding.plain.ui.page.root.components.RootNavigationBar
import com.ismartcoding.plain.ui.page.root.components.RootTabType
import com.ismartcoding.plain.ui.page.root.contents.TabContentAudio
import com.ismartcoding.plain.ui.page.root.contents.TabContentChat
import com.ismartcoding.plain.ui.page.root.contents.TabContentHome
import com.ismartcoding.plain.ui.page.root.contents.TabContentImages
import com.ismartcoding.plain.ui.page.root.contents.TabContentVideos

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RootPagePagerContent(
    navController: NavHostController,
    pagerState: PagerState,
    paddingValues: PaddingValues,
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
    Column(modifier = Modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            beyondViewportPageCount = 3,
            userScrollEnabled = false,
        ) { page ->
            when (page) {
                RootTabType.HOME.value -> TabContentHome(navController, mainVM, paddingValues)
                RootTabType.IMAGES.value -> TabContentImages(
                    states.imagesState,
                    imagesVM,
                    imageTagsVM,
                    imageFoldersVM,
                    imageCastVM,
                    paddingValues,
                )

                RootTabType.AUDIO.value -> TabContentAudio(
                    audioState = states.audioState,
                    audioVM = audioVM,
                    audioPlaylistVM = audioPlaylistVM,
                    tagsVM = audioTagsVM,
                    mediaFoldersVM = audioFoldersVM,
                    castVM = audioCastVM,
                    paddingValues = paddingValues,
                )

                RootTabType.VIDEOS.value -> TabContentVideos(
                    states.videosState,
                    videosVM,
                    videoTagsVM,
                    videoFoldersVM,
                    videoCastVM,
                    paddingValues,
                )

                RootTabType.CHAT.value -> TabContentChat(
                    navController = navController,
                    mainVM = mainVM,
                    peerVM = peerVM,
                    channelVM = channelVM,
                    paddingValues = paddingValues,
                    pagerState = pagerState,
                )
            }
        }

        RootNavigationBar(
            selectedTab = pagerState.currentPage,
            onTabSelected = onTabSelected,
        )
    }
}