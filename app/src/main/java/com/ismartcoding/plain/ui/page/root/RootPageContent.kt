package com.ismartcoding.plain.ui.page.root

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
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
import com.ismartcoding.plain.ui.page.root.contents.PageContentApps
import com.ismartcoding.plain.ui.page.root.contents.PageContentAudio
import com.ismartcoding.plain.ui.page.root.contents.PageContentChat
import com.ismartcoding.plain.ui.page.root.contents.PageContentDocs
import com.ismartcoding.plain.ui.page.root.contents.PageContentFeeds
import com.ismartcoding.plain.ui.page.root.contents.PageContentFiles
import com.ismartcoding.plain.ui.page.root.contents.PageContentHome
import com.ismartcoding.plain.ui.page.root.contents.PageContentImages
import com.ismartcoding.plain.ui.page.root.contents.PageContentNotes
import com.ismartcoding.plain.ui.page.root.contents.PageContentPomodoro
import com.ismartcoding.plain.ui.page.root.contents.PageContentSoundMeter
import com.ismartcoding.plain.ui.page.root.contents.PageContentVideos

@Composable
fun RootPageContent(
    navController: NavHostController,
    currentPage: Int,
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
    notesVM: NotesViewModel,
    noteTagsVM: TagsViewModel,
    feedTagsVM: TagsViewModel,
    pomodoroVM: PomodoroViewModel,
    onOpenDrawer: () -> Unit,
) {
    when (currentPage) {
        RootPageType.HOME.value -> PageContentHome(navController, mainVM, paddingValues)
        RootPageType.IMAGES.value -> PageContentImages(
            states.imagesState,
            imagesVM,
            imageTagsVM,
            imageFoldersVM,
            imageCastVM,
            paddingValues,
        )

        RootPageType.AUDIO.value -> PageContentAudio(
            audioState = states.audioState,
            audioVM = audioVM,
            audioPlaylistVM = audioPlaylistVM,
            tagsVM = audioTagsVM,
            mediaFoldersVM = audioFoldersVM,
            castVM = audioCastVM,
            paddingValues = paddingValues,
        )

        RootPageType.VIDEOS.value -> PageContentVideos(
            states.videosState,
            videosVM,
            videoTagsVM,
            videoFoldersVM,
            videoCastVM,
            paddingValues,
        )

        RootPageType.CHAT.value -> PageContentChat(
            navController = navController,
            mainVM = mainVM,
            peerVM = peerVM,
            channelVM = channelVM,
            paddingValues = paddingValues,
            currentPage = currentPage,
        )

        RootPageType.FILES.value -> PageContentFiles(
            navController = navController,
            audioPlaylistVM = audioPlaylistVM,
            onOpenDrawer = onOpenDrawer,
            paddingValues = paddingValues,
        )

        RootPageType.DOCS.value -> PageContentDocs(
            navController = navController,
            onOpenDrawer = onOpenDrawer,
            paddingValues = paddingValues,
        )

        RootPageType.NOTES.value -> PageContentNotes(
            navController = navController,
            notesVM = notesVM,
            tagsVM = noteTagsVM,
            onOpenDrawer = onOpenDrawer,
            paddingValues = paddingValues,
        )

        RootPageType.FEEDS.value -> PageContentFeeds(
            navController = navController,
            tagsVM = feedTagsVM,
            onOpenDrawer = onOpenDrawer,
            paddingValues = paddingValues,
        )

        RootPageType.APPS.value -> PageContentApps(
            navController = navController,
            onOpenDrawer = onOpenDrawer,
            paddingValues = paddingValues,
        )

        RootPageType.POMODORO.value -> PageContentPomodoro(
            navController = navController,
            pomodoroVM = pomodoroVM,
            onOpenDrawer = onOpenDrawer,
            paddingValues = paddingValues,
        )

        RootPageType.SOUND_METER.value -> PageContentSoundMeter(
            navController = navController,
            onOpenDrawer = onOpenDrawer,
            paddingValues = paddingValues,
        )
    }
}
