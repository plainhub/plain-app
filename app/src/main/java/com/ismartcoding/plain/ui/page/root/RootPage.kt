package com.ismartcoding.plain.ui.page.root

import androidx.activity.compose.BackHandler
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.base.LocalOpenDrawer
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.ChannelViewModel
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.NotesViewModel
import com.ismartcoding.plain.ui.models.PeerViewModel
import com.ismartcoding.plain.ui.models.PomodoroViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VideosViewModel
import com.ismartcoding.plain.ui.page.root.components.RootPageType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RootPage(
    navController: NavHostController,
    mainVM: MainViewModel,
    imagesVM: ImagesViewModel = viewModel(key = "imagesVM"),
    imageTagsVM: TagsViewModel = viewModel(key = "imageTagsVM"),
    imageFoldersVM: MediaFoldersViewModel = viewModel(key = "imageFoldersVM"),
    imageCastVM: CastViewModel = viewModel(key = "imageCastVM"),
    videosVM: VideosViewModel = viewModel(key = "videosVM"),
    videoTagsVM: TagsViewModel = viewModel(key = "videoTagsVM"),
    videoFoldersVM: MediaFoldersViewModel = viewModel(key = "videoFoldersVM"),
    videoCastVM: CastViewModel = viewModel(key = "videoCastVM"),
    audioVM: AudioViewModel = viewModel(key = "audioVM"),
    audioTagsVM: TagsViewModel = viewModel(key = "audioTagsVM"),
    audioFoldersVM: MediaFoldersViewModel = viewModel(key = "audioFoldersVM"),
    audioCastVM: CastViewModel = viewModel(key = "audioCastVM"),
    audioPlaylistVM: AudioPlaylistViewModel,
    peerVM: PeerViewModel = viewModel(key = "peerVM"),
    channelVM: ChannelViewModel = viewModel(key = "channelVM"),
    notesVM: NotesViewModel = viewModel(key = "notesVM"),
    noteTagsVM: TagsViewModel = viewModel(key = "noteTagsVM"),
    feedTagsVM: TagsViewModel = viewModel(key = "feedTagsVM"),
    pomodoroVM: PomodoroViewModel = viewModel(key = "pomodoroVM"),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val openDrawer = LocalOpenDrawer.current

    BackHandler(
        enabled = mainVM.currentRootTab != RootPageType.HOME.value,
    ) {
        mainVM.currentRootTab = RootPageType.HOME.value
    }

    val states = rememberRootPageStates(
        imagesVM = imagesVM,
        imageTagsVM = imageTagsVM,
        imageFoldersVM = imageFoldersVM,
        videosVM = videosVM,
        videoTagsVM = videoTagsVM,
        videoFoldersVM = videoFoldersVM,
        audioVM = audioVM,
        audioTagsVM = audioTagsVM,
        audioFoldersVM = audioFoldersVM,
    )

    RootPageBackHandler(
        currentPage = mainVM.currentRootTab,
        states = states,
        context = context,
        scope = scope,
        imagesVM = imagesVM,
        imageTagsVM = imageTagsVM,
        imageCastVM = imageCastVM,
        videosVM = videosVM,
        videoTagsVM = videoTagsVM,
        videoCastVM = videoCastVM,
        audioVM = audioVM,
        audioTagsVM = audioTagsVM,
        audioCastVM = audioCastVM,
    )

    RootPageScaffoldContent(
        navController = navController,
        currentPage = mainVM.currentRootTab,
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
        notesVM = notesVM,
        noteTagsVM = noteTagsVM,
        feedTagsVM = feedTagsVM,
        pomodoroVM = pomodoroVM,
        onOpenDrawer = { scope.launch { openDrawer?.invoke() } },
    )

    RootMediaPreviewers(
        states = states,
        scope = scope,
        context = context,
        imagesVM = imagesVM,
        imageTagsVM = imageTagsVM,
        videosVM = videosVM,
        videoTagsVM = videoTagsVM,
    )
}
