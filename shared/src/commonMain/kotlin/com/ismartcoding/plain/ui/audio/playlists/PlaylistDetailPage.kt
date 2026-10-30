package com.ismartcoding.plain.ui.audio.playlists

import com.ismartcoding.plain.models.AudioAlbumCover
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.IDData
import com.ismartcoding.plain.Prefs
import com.ismartcoding.plain.models.AudioItem
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.i18n.sort
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.ui.navigation.PBackHandler
import com.ismartcoding.plain.platform.media.audioIsPlayingFlow
import com.ismartcoding.plain.platform.media.audioPause
import com.ismartcoding.plain.platform.media.audioPlay
import com.ismartcoding.plain.preferences.*
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.components.*
import com.ismartcoding.plain.ui.base.dragselect.listDragSelect
import com.ismartcoding.plain.ui.base.dragselect.rememberListDragSelectState
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.ui.media.MediaTopBar
import com.ismartcoding.plain.ui.audio.components.PlaylistNameDialog
import com.ismartcoding.plain.ui.components.DialogHelper
import com.ismartcoding.plain.ui.audio.AudioQueueViewModel
import com.ismartcoding.plain.ui.audio.AudioViewModel
import com.ismartcoding.plain.ui.casting.CastViewModel
import com.ismartcoding.plain.ui.files.MediaFoldersViewModel
import com.ismartcoding.plain.ui.tags.TagsViewModel
import com.ismartcoding.plain.ui.navigation.Routing
import com.ismartcoding.plain.ui.audio.components.AudioFilesSelectModeBottomActions
import com.ismartcoding.plain.ui.audio.components.ViewAudioBottomSheet
import com.ismartcoding.plain.ui.audio.player.components.AudioPlayerBar
import com.ismartcoding.plain.ui.casting.CastDialog
import com.ismartcoding.plain.ui.casting.CastQueueFab
import com.ismartcoding.plain.ui.audio.playlists.components.PlaylistMoreSheet
import com.ismartcoding.plain.ui.audio.playlists.components.pruneTrashedRows
import com.ismartcoding.plain.ui.audio.playlists.components.toAudioTrack
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.cast as ui_drawable_cast
import com.ismartcoding.plain.ui.resources.more_horiz as ui_drawable_more_horiz
import com.ismartcoding.plain.ui.resources.sort as ui_drawable_sort
import com.ismartcoding.plain.ui.state.launchSafe
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailPage(
    navController: NavHostController,
    playlistId: String,
    audioQueueVM: AudioQueueViewModel,
    audioVM: AudioViewModel,
    tagsVM: TagsViewModel,
    mediaFoldersVM: MediaFoldersViewModel,
    castVM: CastViewModel,
) {
    val scope = rememberCoroutineScope()
    val isPlaying by audioIsPlayingFlow().collectAsState()
    val library by audioVM.itemsFlow.collectAsState()
    val tagsState by tagsVM.itemsFlow.collectAsState()
    val tagsMapState by tagsVM.tagsMapFlow.collectAsState()
    val scrollState = rememberLazyListState()
    val dragSelectState = rememberListDragSelectState({ scrollState })
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(canScroll = {
        scrollState.firstVisibleItemIndex > 0 && !dragSelectState.selectMode
    })

    var playlistName by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<AudioItem>>(listOf()) }
    var albumCovers by remember { mutableStateOf<List<AudioAlbumCover>>(emptyList()) }
    var version by remember { mutableIntStateOf(0) }
    var showMore by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }

    val reload: suspend () -> Unit = {
        val (name, list) = withIO {
            audioVM.source.playlists.playlist(playlistId)?.name to audioVM.source.playlists.playlistItemsPage(playlistId, 0, 1000)
        }
        playlistName = name ?: ""
        items = list.pruneTrashedRows(playlistId, audioVM.source)
    }

    LaunchedEffect(playlistId, version) {
        reload()
    }

    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch {
            reload()
            setRefreshState(RefreshContentState.Finished)
        }
    }

    LaunchedEffect(items) {
        albumCovers = if (items.isEmpty()) emptyList() else audioVM.source.playlists.covers(listOf(playlistId))[playlistId].orEmpty()
    }
    PlaylistResumeEffect { version++ }

    val sortBy = audioVM.sortBy.value
    val libraryByPath = remember(library) { library.associateBy { it.path } }
    val rowAudios = remember(items, library, sortBy) {
        items.map { libraryByPath[it.path] ?: it.toAudioTrack() }
    }
    val selectItems = remember(rowAudios) { rowAudios.map { IDData(it.id) } }
    val pathById = remember(library) { library.associateBy { it.id } }
    val density = LocalDensity.current
    var playerBarClearance by remember { mutableStateOf(0.dp) }
    val audioOutput by com.ismartcoding.plain.features.audio.AudioCommands.output.collectAsState()
    val contextActive = audioOutput.source.key == audioVM.source.key && items.isNotEmpty() && audioQueueVM.queuedMediaIds.value == items.mapNotNull { it.mediaId }.toSet()
    val isPlayingContext = contextActive && isPlaying

    PBackHandler(enabled = dragSelectState.selectMode || castVM.castMode.value) {
        when {
            dragSelectState.selectMode -> dragSelectState.exitSelectMode()
            castVM.castMode.value -> castVM.exitCastMode()
        }
    }

    val renamedMsg = stringResource(Res.string.renamed)
    val deletedMsg = stringResource(Res.string.playlist_deleted)
    if (showRename) {
        PlaylistNameDialog(
            title = stringResource(Res.string.rename_playlist),
            initial = playlistName,
            confirmText = stringResource(Res.string.save),
            onConfirm = { name ->
                showRename = false
                scope.launch {
                    withIO { audioVM.source.playlists.renamePlaylist(playlistId, name) }
                    DialogHelper.showMessage(renamedMsg)
                    version++
                }
            },
            onDismiss = { showRename = false },
        )
    }
    if (showMore) {
        PlaylistMoreSheet(
            playlistName = playlistName,
            itemCount = items.size,
            onDismiss = { showMore = false },
            onRename = { showRename = true },
            onAddItems = { navController.navigate(Routing.PlaylistAddItems(playlistId)) },
            onDelete = {
                scope.launch {
                    withIO { audioVM.source.playlists.deletePlaylist(playlistId) }
                    DialogHelper.showMessage(deletedMsg)
                    navController.popBackStack()
                }
            },
        )
    }
    ViewAudioBottomSheet(
        audioVM = audioVM,
        tagsVM = tagsVM,
        tagsMapState = tagsMapState,
        tagsState = tagsState,
        dragSelectState = dragSelectState,
        castVM = castVM,
        playlistId = playlistId,
        onPlaylistChanged = { version++ },
    )
    CastDialog(castVM)

    MediaTopBar(
        navController = navController,
        mediaVM = audioVM,
        tagsVM = tagsVM,
        castVM = castVM,
        mediaFoldersVM = mediaFoldersVM,
        dragSelectState = dragSelectState,
        scrollBehavior = scrollBehavior,
        showSidebar = false,
        bucketsMap = emptyMap(),
        itemsState = rowAudios,
        scrollToTop = { scope.launch { scrollState.scrollToItem(0) } },
        onSortSelected = { sortBy ->
            scope.launch {
                Prefs.set(PrefKeys.AUDIO_SORT_BY, sortBy.name)
                audioVM.sortBy.value = sortBy
            }
        },
        onSearchAction = { },
        navigationIcon = {
            NavigationBackIcon { navController.navigateUp() }
        },
        topBarActions = {
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
            PIconButton(
                icon = UiRes.drawable.ui_drawable_more_horiz,
                contentDescription = stringResource(Res.string.more),
                click = { showMore = true },
            )
        },
        bottomBar = {
            AnimatedBottomAction(visible = dragSelectState.showBottomActions()) {
                AudioFilesSelectModeBottomActions(
                    audioVM = audioVM, audioQueueVM = audioQueueVM,
                    tagsVM = tagsVM,
                    tagsState = tagsState,
                    dragSelectState = dragSelectState,
                    items = rowAudios,
                    removeFromPlaylist = {
                        scope.launch {
                            val paths = dragSelectState.selectedIds.toList()
                            if (paths.isNotEmpty()) {
                                withIO { audioVM.source.playlists.removePlaylistItems(playlistId, paths) }
                                dragSelectState.exitSelectMode()
                                DialogHelper.showMessage(Res.string.removed_from_playlist)
                                version++
                            }
                        }
                    },
                )
            }
        },
    ) { paddingValues ->
        // Only the top inset goes on the container; the player bar carries its
        // own navigationBarsPadding and must sit flush at the screen bottom
        // (same pattern as AudioAllPage).
        Box(Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding())) {
            PullToRefresh(
                refreshLayoutState = topRefreshLayoutState,
                userEnable = !dragSelectState.selectMode,
                modifier = Modifier.fillMaxSize(),
            ) {
                PlaylistTracksList(
                    name = playlistName, itemCount = items.size,
                    albums = albumCovers, gradientIndex = playlistId.hashCode(),
                    rowAudios = rowAudios,
                    audioVM = audioVM, audioQueueVM = audioQueueVM,
                    tagsVM = tagsVM, castVM = castVM,
                    dragSelectState = dragSelectState,
                    isPlaying = isPlaying, isPlayingContext = isPlayingContext,
                    state = scrollState,
                    bottomClearance = maxOf(playerBarClearance, 40.dp + paddingValues.calculateBottomPadding()),
                    modifier = Modifier.fillMaxSize()
                        .nestedScroll(scrollBehavior.nestedScrollConnection)
                        .listDragSelect(items = selectItems, state = dragSelectState, itemIndexOffset = 2),
                    onPlayAll = {
                        if (contextActive) {
                            if (isPlaying) audioPause() else audioPlay()
                        } else {
                            scope.launchSafe { audioQueueVM.playQueryAsync("playlist_id:$playlistId") }
                        }
                    },
                    onShuffle = { scope.launchSafe { audioQueueVM.playQueryAsync("playlist_id:$playlistId", shuffle = true) } },
                )
            }
            AudioPlayerBar(
                audioQueueVM, castVM,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { playerBarClearance = with(density) { it.height.toDp() } },
                dragSelectState = dragSelectState,
            )
            CastQueueFab(castVM = castVM, modifier = Modifier.align(Alignment.BottomEnd), dragSelectState = dragSelectState)
        }
    }
}
