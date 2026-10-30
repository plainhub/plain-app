package com.ismartcoding.plain.ui.audio

import com.ismartcoding.plain.preferences.PrefKeys

/** The flat "all items" list, kept as the [com.ismartcoding.plain.ui.navigation.Routing.AudioAll] page. */

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.cast_mode
import com.ismartcoding.plain.i18n.sort
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.platform.media.audioIsPlayingFlow
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.ui.components.ActionButtonSearch
import com.ismartcoding.plain.ui.base.AnimatedBottomAction
import com.ismartcoding.plain.ui.components.NavigationBackIcon
import com.ismartcoding.plain.ui.components.NeedPermissionColumn
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.media.MediaTopBar
import com.ismartcoding.plain.ui.casting.CastViewModel
import com.ismartcoding.plain.ui.files.MediaFoldersViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.state.enterSearchMode
import com.ismartcoding.plain.ui.state.exitSearchMode
import com.ismartcoding.plain.ui.audio.components.AudioFilesSelectModeBottomActions
import com.ismartcoding.plain.ui.audio.components.ViewAudioBottomSheet
import com.ismartcoding.plain.ui.audio.player.components.AudioPlayerBar
import com.ismartcoding.plain.ui.casting.CastDialog
import com.ismartcoding.plain.ui.casting.CastQueueFab
import com.ismartcoding.plain.ui.tags.TagsBottomSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.cast as ui_drawable_cast
import com.ismartcoding.plain.ui.resources.music as ui_drawable_music
import com.ismartcoding.plain.ui.resources.sort as ui_drawable_sort

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun AudioAllPage(
    navController: NavHostController,
    audioQueueVM: AudioQueueViewModel,
    audioVM: AudioViewModel,
    tagsVM: TagsViewModel,
    mediaFoldersVM: MediaFoldersViewModel,
    castVM: CastViewModel,
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
    val density = LocalDensity.current
    var playerBarClearance by remember { mutableStateOf(0.dp) }
    audioVM.scrollStateMap[0] = scrollState

    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch {
            audioVM.loadAsync(tagsVM)
            audioQueueVM.loadAsync()
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

    AudioPageEffects(audioState, audioVM, audioQueueVM, tagsVM, mediaFoldersVM)

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
                        Prefs.set(PrefKeys.AUDIO_SORT_BY, sortBy.name)
                        audioVM.sortBy.value = sortBy
                        audioVM.loadAsync(tagsVM)
                    }
                },
                onSearchAction = { tv ->
                    scope.launch(Dispatchers.Default) {
                        audioVM.loadAsync(tv)
                    }
                },
                // Sub-page: back arrow instead of the folder drawer; trailing
                // actions add sort + cast on top of search (the flat list is
                // the cast source, so unlike home it keeps a cast entry).
                navigationIcon = {
                    NavigationBackIcon { navController.navigateUp() }
                },
                topBarActions = {
                    ActionButtonSearch { audioVM.enterSearchMode() }
                    PIconButton(
                        icon = UiRes.drawable.ui_drawable_sort,
                        contentDescription = stringResource(Res.string.sort),
                        click = { audioVM.showSortAndBrowseDialog.value = true },
                    )
                    PIconButton(
                        icon = UiRes.drawable.ui_drawable_cast,
                        contentDescription = stringResource(Res.string.cast_mode),
                        click = { castVM.showCastDialog.value = true },
                    )
                },
        bottomBar = {
            AnimatedBottomAction(visible = dragSelectState.showBottomActions()) {
                AudioFilesSelectModeBottomActions(audioVM, audioQueueVM, tagsVM, tagsState, dragSelectState, itemsState)
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
                    NeedPermissionColumn(UiRes.drawable.ui_drawable_music, AppFeatureType.FILES.getPermission()!!); return@Column
                }

                AudioPageList(
                    scrollBehavior, dragSelectState, itemsState, audioVM, audioQueueVM,
                    tagsVM, castVM, audioTagsMap, isAudioPlaying, topRefreshLayoutState, paddingValues,
                    extraBottomPadding = playerBarClearance,
                )
            }
            AudioPlayerBar(audioQueueVM, castVM, modifier = Modifier
                .align(Alignment.BottomCenter)
                .onSizeChanged { playerBarClearance = with(density) { it.height.toDp() } }, dragSelectState = audioState.dragSelectState)
            CastQueueFab(castVM = castVM, modifier = Modifier.align(Alignment.BottomEnd), dragSelectState = audioState.dragSelectState)
        }
    }
}
