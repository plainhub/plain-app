package com.ismartcoding.plain.ui.page.audio

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.platform.PBackHandler
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.audioIsPlayingFlow
import com.ismartcoding.plain.preferences.AudioSortByPreference
import com.ismartcoding.plain.ui.base.AnimatedBottomAction
import com.ismartcoding.plain.ui.base.MediaTopBar
import com.ismartcoding.plain.ui.base.NeedPermissionColumn
import com.ismartcoding.plain.ui.base.PFilterChip
import com.ismartcoding.plain.ui.base.PScrollableTabRow
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.extensions.reset
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.MediaFoldersViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.models.VTabData
import com.ismartcoding.plain.ui.models.exitSearchMode
import com.ismartcoding.plain.ui.page.audio.components.AudioFilesSelectModeBottomActions
import com.ismartcoding.plain.ui.page.audio.components.AudioPlayerBar
import com.ismartcoding.plain.ui.page.audio.components.ViewAudioBottomSheet
import com.ismartcoding.plain.ui.page.cast.AudioCastPlayerBar
import com.ismartcoding.plain.ui.page.cast.CastDialog
import com.ismartcoding.plain.ui.page.tags.TagsBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AudioPage(
    navController: NavHostController,
    audioPlaylistVM: AudioPlaylistViewModel,
    audioVM: AudioViewModel = viewModel(key = "audioVM") { AudioViewModel() },
    tagsVM: TagsViewModel = viewModel(key = "audioTagsVM") { TagsViewModel() },
    mediaFoldersVM: MediaFoldersViewModel = viewModel(key = "audioFoldersVM") { MediaFoldersViewModel() },
    castVM: CastViewModel = viewModel(key = "audioCastVM") { CastViewModel() },
) {
    val scope = rememberCoroutineScope()
    val audioState = AudioPageState.create(audioVM, tagsVM, mediaFoldersVM)
    val scrollBehavior = audioState.scrollBehavior
    val tagsState = audioState.tagsState
    val tagsMapState = audioState.tagsMapState
    val dragSelectState = audioState.dragSelectState
    val itemsState = audioState.itemsState
    val scrollState = audioState.scrollState
    val isAudioPlaying by audioIsPlayingFlow().collectAsState()
    audioVM.scrollStateMap[0] = scrollState

    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch {
            audioVM.loadAsync(tagsVM)
            audioPlaylistVM.loadAsync()
            withIO { mediaFoldersVM.loadAsync() }
            setRefreshState(RefreshContentState.Finished)
        }
    }

    PBackHandler(enabled = dragSelectState.selectMode || castVM.castMode.value || audioVM.showSearchBar.value) {
        when {
            dragSelectState.selectMode -> dragSelectState.exitSelectMode()
            castVM.castMode.value -> castVM.exitCastMode()
            audioVM.showSearchBar.value && (!audioVM.searchActive.value || audioVM.queryText.value.isEmpty()) -> {
                audioVM.exitSearchMode()
                audioVM.showLoading.value = true
                scope.launch(Dispatchers.Default) { audioVM.loadAsync(tagsVM) }
            }
        }
    }

    AudioPageEffects(audioState, audioVM, audioPlaylistVM, tagsVM, mediaFoldersVM)

    val audioTagsMap = remember(tagsMapState, tagsState) {
        tagsMapState.mapValues { entry -> entry.value.mapNotNull { relation -> tagsState.find { it.id == relation.tagId } } }
    }

    ViewAudioBottomSheet(audioVM = audioVM, tagsVM = tagsVM, tagsMapState = tagsMapState, tagsState = tagsState, dragSelectState = dragSelectState, castVM = castVM)
    if (audioVM.showTagsDialog.value) {
        TagsBottomSheet(tagsVM) { audioVM.showTagsDialog.value = false }
    }
    CastDialog(castVM)

    MediaTopBar(
        navController = navController,
        mediaVM = audioVM,
        tagsVM = tagsVM,
        castVM = castVM,
        mediaFoldersVM = mediaFoldersVM,
        dragSelectState = dragSelectState,
                scrollBehavior = scrollBehavior,
                bucketsMap = audioState.bucketsMap,
                itemsState = itemsState,
                scrollToTop = { scope.launch { scrollState.scrollToItem(0) } },
                onSortSelected = { sortBy ->
                    scope.launch(Dispatchers.Default) {
                        AudioSortByPreference.putAsync(sortBy)
                        audioVM.sortBy.value = sortBy
                        audioVM.loadAsync(tagsVM)
                    }
                },
                onSearchAction = { tv ->
                    scope.launch(Dispatchers.Default) {
                        audioVM.loadAsync(tv)
                    }
                },
        bottomBar = {
            AnimatedBottomAction(visible = dragSelectState.showBottomActions()) {
                AudioFilesSelectModeBottomActions(audioVM, audioPlaylistVM, tagsVM, tagsState, dragSelectState)
            }
        },
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                if (!audioVM.hasPermission.value) {
                    NeedPermissionColumn(Res.drawable.music, AppFeatureType.FILES.getPermission()!!); return@Column
                }

                AudioPageList(
                    scrollBehavior, dragSelectState, itemsState, audioVM, audioPlaylistVM,
                    tagsVM, castVM, audioTagsMap, isAudioPlaying, topRefreshLayoutState, paddingValues
                )
            }
            AudioPlayerBar(audioPlaylistVM, castVM, modifier = Modifier.align(Alignment.BottomCenter), dragSelectState = audioState.dragSelectState)
            AudioCastPlayerBar(castVM = castVM, modifier = Modifier.align(Alignment.BottomCenter), dragSelectState = audioState.dragSelectState)
        }
    }
}
