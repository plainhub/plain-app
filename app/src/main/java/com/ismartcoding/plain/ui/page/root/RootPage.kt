package com.ismartcoding.plain.ui.page.root

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.ui.base.AnimatedBottomAction
import com.ismartcoding.plain.ui.base.PScaffold
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.MediaPreviewer
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.ChatListViewModel
import com.ismartcoding.plain.ui.models.ImagesViewModel
import com.ismartcoding.plain.ui.models.MainViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VideosViewModel
import com.ismartcoding.plain.ui.models.exitSearchMode
import com.ismartcoding.plain.ui.page.audio.components.AudioFilesSelectModeBottomActions
import com.ismartcoding.plain.ui.page.audio.AudioPageState
import com.ismartcoding.plain.ui.page.images.ImageFilesSelectModeBottomActions
import com.ismartcoding.plain.ui.page.images.ImagesPageState
import com.ismartcoding.plain.ui.page.root.components.RootNavigationBar
import com.ismartcoding.plain.ui.page.root.components.RootTabType
import com.ismartcoding.plain.ui.page.root.contents.TabContentAudio
import com.ismartcoding.plain.ui.page.root.contents.TabContentChat
import com.ismartcoding.plain.ui.page.root.contents.TabContentHome
import com.ismartcoding.plain.ui.page.root.contents.TabContentImages
import com.ismartcoding.plain.ui.page.root.contents.TabContentVideos
import com.ismartcoding.plain.ui.page.root.topbars.TopBarAudio
import com.ismartcoding.plain.ui.page.root.topbars.TopBarChat
import com.ismartcoding.plain.ui.page.root.topbars.TopBarHome
import com.ismartcoding.plain.ui.page.root.topbars.TopBarImages
import com.ismartcoding.plain.ui.page.root.topbars.TopBarVideos
import com.ismartcoding.plain.ui.page.videos.VideoFilesSelectModeBottomActions
import com.ismartcoding.plain.ui.page.videos.VideosPageState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
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
    audioPlaylistVM: AudioPlaylistViewModel = viewModel(key = "audioPlaylistVM"),
    audioFoldersVM: MediaFoldersViewModel = viewModel(key = "audioFoldersVM"),
    audioCastVM: CastViewModel = viewModel(key = "audioCastVM"),
    chatListVM: ChatListViewModel = viewModel(key = "chatListVM"),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(
        initialPage = RootTabType.HOME.value,
        pageCount = { 5 }
    )

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
        if (pagerState.currentPage == RootTabType.IMAGES.value) {
            if (imagesState.previewerState.visible) {
                scope.launch {
                    imagesState.previewerState.closeTransform()
                }
            } else if (imagesState.dragSelectState.selectMode) {
                imagesState.dragSelectState.exitSelectMode()
            } else if (imageCastVM.castMode.value) {
                imageCastVM.exitCastMode()
            } else if (imagesVM.showSearchBar.value) {
                if (!imagesVM.searchActive.value || imagesVM.queryText.value.isEmpty()) {
                    imagesVM.exitSearchMode()
                    imagesVM.showLoading.value = true
                    scope.launch(Dispatchers.IO) {
                        imagesVM.loadAsync(context, imageTagsVM)
                    }
                }
            }
        } else if (pagerState.currentPage == RootTabType.VIDEOS.value) {
            if (videosState.previewerState.visible) {
                scope.launch {
                    videosState.previewerState.closeTransform()
                }
            } else if (videosState.dragSelectState.selectMode) {
                videosState.dragSelectState.exitSelectMode()
            } else if (videoCastVM.castMode.value) {
                videoCastVM.exitCastMode()
            } else if (videosVM.showSearchBar.value) {
                if (!videosVM.searchActive.value || videosVM.queryText.value.isEmpty()) {
                    videosVM.exitSearchMode()
                    videosVM.showLoading.value = true
                    scope.launch(Dispatchers.IO) {
                        videosVM.loadAsync(context, videoTagsVM)
                    }
                }
            }
        } else if (pagerState.currentPage == RootTabType.AUDIO.value) {
            if (audioState.dragSelectState.selectMode) {
                audioState.dragSelectState.exitSelectMode()
            } else if (audioCastVM.castMode.value) {
                audioCastVM.exitCastMode()
            } else if (audioVM.showSearchBar.value) {
                if (!audioVM.searchActive.value || audioVM.queryText.value.isEmpty()) {
                    audioVM.exitSearchMode()
                    audioVM.showLoading.value = true
                    scope.launch(Dispatchers.IO) {
                        audioVM.loadAsync(context, audioTagsVM)
                    }
                }
            }
        }
    }

    PScaffold(
        topBar = {
            when (pagerState.currentPage) {
                RootTabType.HOME.value -> {
                    TopBarHome(
                        navController = navController,
                    )
                }

                RootTabType.IMAGES.value -> {
                    TopBarImages(
                        navController = navController,
                        imagesState = imagesState,
                        imagesVM = imagesVM,
                        tagsVM = imageTagsVM,
                        castVM = imageCastVM,
                    )
                }

                RootTabType.AUDIO.value -> {
                    TopBarAudio(
                        navController = navController,
                        audioState = audioState,
                        audioVM = audioVM,
                        tagsVM = audioTagsVM,
                        castVM = audioCastVM,
                    )
                }

                RootTabType.VIDEOS.value -> {
                    TopBarVideos(
                        navController = navController,
                        videosState = videosState,
                        videosVM = videosVM,
                        tagsVM = videoTagsVM,
                        castVM = videoCastVM,
                    )
                }

                RootTabType.CHAT.value -> {
                    TopBarChat(
                        navController = navController
                    )
                }
            }
        },
        bottomBar = {
            AnimatedBottomAction(visible = imagesState.dragSelectState.showBottomActions()) {
                ImageFilesSelectModeBottomActions(
                    imagesVM,
                    imageTagsVM,
                    imagesState.tagsState,
                    imagesState.dragSelectState
                )
            }
            AnimatedBottomAction(visible = videosState.dragSelectState.showBottomActions()) {
                VideoFilesSelectModeBottomActions(
                    videosVM,
                    videoTagsVM,
                    videosState.tagsState,
                    videosState.dragSelectState
                )
            }
            AnimatedBottomAction(visible = audioState.dragSelectState.showBottomActions()) {
                AudioFilesSelectModeBottomActions(
                    audioVM,
                    audioPlaylistVM,
                    audioTagsVM,
                    audioState.tagsState,
                    audioState.dragSelectState
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                beyondViewportPageCount = 3,
                userScrollEnabled = false,
            ) { page ->
                when (page) {
                    RootTabType.HOME.value -> {
                        TabContentHome(
                            navController = navController,
                            mainVM = mainVM,
                            paddingValues
                        )
                    }

                    RootTabType.IMAGES.value -> {
                        TabContentImages(
                            imagesState,
                            imagesVM,
                            imageTagsVM, imageFoldersVM, imageCastVM, paddingValues
                        )
                    }

                    RootTabType.AUDIO.value -> {
                        TabContentAudio(
                            audioState = audioState,
                            audioVM = audioVM,
                            audioPlaylistVM = audioPlaylistVM,
                            tagsVM = audioTagsVM,
                            mediaFoldersVM = audioFoldersVM,
                            castVM = audioCastVM,
                            paddingValues = paddingValues
                        )
                    }

                    RootTabType.VIDEOS.value -> {
                        TabContentVideos(
                            videosState,
                            videosVM, videoTagsVM, videoFoldersVM, videoCastVM, paddingValues
                        )
                    }

                    RootTabType.CHAT.value -> {
                        TabContentChat(
                            navController = navController,
                            chatListVM = chatListVM,
                            paddingValues = paddingValues,
                            pagerState = pagerState,
                        )
                    }
                }
            }

            RootNavigationBar(
                selectedTab = pagerState.currentPage,
                onTabSelected = {
                    scope.launch {
                        pagerState.animateScrollToPage(it)
                    }
                }
            )
        }
    }

    MediaPreviewer(
        state = imagesState.previewerState,
        tagsVM = imageTagsVM,
        tagsMap = imagesState.tagsMapState,
        tagsState = imagesState.tagsState,
        onRenamed = {
            scope.launch(Dispatchers.IO) {
                imagesVM.loadAsync(context, imageTagsVM)
            }
        },
        deleteAction = { item ->
            scope.launch(Dispatchers.IO) {
                imagesVM.delete(context, imageTagsVM, setOf(item.mediaId))
                imagesState.previewerState.closeTransform()
            }
        },
        onTagsChanged = {
        }
    )

    MediaPreviewer(
        state = videosState.previewerState,
        tagsVM = videoTagsVM,
        tagsMap = videosState.tagsMapState,
        tagsState = videosState.tagsState,
        onRenamed = {
            scope.launch(Dispatchers.IO) {
                videosVM.loadAsync(context, videoTagsVM)
            }
        },
        deleteAction = { item ->
            scope.launch(Dispatchers.IO) {
                videosVM.delete(context, videoTagsVM, setOf(item.mediaId))
                videosState.previewerState.closeTransform()
            }
        },
        onTagsChanged = {
        }
    )
}

