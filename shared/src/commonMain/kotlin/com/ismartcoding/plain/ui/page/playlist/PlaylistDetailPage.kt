package com.ismartcoding.plain.ui.page.playlist

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.navigation.NavHostController
import com.ismartcoding.plain.db.DAudioPlaylistItem
import com.ismartcoding.plain.db.IDData
import com.ismartcoding.plain.features.audio.AudioPlaylistManager
import com.ismartcoding.plain.features.audio.AudioQueueManager
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.PBackHandler
import com.ismartcoding.plain.platform.audioIsPlayingFlow
import com.ismartcoding.plain.platform.audioJustPlayWithNotificationCheck
import com.ismartcoding.plain.platform.audioPause
import com.ismartcoding.plain.platform.audioPlay
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.base.dragselect.listDragSelect
import com.ismartcoding.plain.ui.base.dragselect.rememberListDragSelectState
import com.ismartcoding.plain.ui.base.pullrefresh.PullToRefresh
import com.ismartcoding.plain.ui.base.pullrefresh.RefreshContentState
import com.ismartcoding.plain.ui.base.pullrefresh.rememberRefreshLayoutState
import com.ismartcoding.plain.ui.base.pullrefresh.setRefreshState
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.ui.components.PlaylistNameDialog
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.audio.components.AudioFilesSelectModeBottomActions
import com.ismartcoding.plain.ui.page.audio.components.AudioListItem
import com.ismartcoding.plain.ui.page.audio.components.ViewAudioBottomSheet
import com.ismartcoding.plain.ui.page.audioplayer.components.AudioPlayerBar
import com.ismartcoding.plain.ui.page.cast.AudioCastPlayerBar
import com.ismartcoding.plain.ui.page.cast.CastDialog
import com.ismartcoding.plain.ui.page.playlist.components.PlaylistActionsRow
import com.ismartcoding.plain.ui.page.playlist.components.PlaylistHeaderRow
import com.ismartcoding.plain.ui.page.playlist.components.PlaylistMoreSheet
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

/** Playlist detail page: state, load/reload, and wiring; pieces live in components/. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailPage(
    navController: NavHostController,
    playlistId: String,
    audioPlaylistVM: AudioPlaylistViewModel,
    audioVM: AudioViewModel,
    tagsVM: TagsViewModel,
    castVM: CastViewModel,
) {
    val scope = rememberCoroutineScope()
    val isPlaying by audioIsPlayingFlow().collectAsState()
    val library by audioVM.itemsFlow.collectAsState()
    val tagsState by tagsVM.itemsFlow.collectAsState()
    val tagsMapState by tagsVM.tagsMapFlow.collectAsState()
    val scrollState = rememberLazyListState()
    val dragSelectState = rememberListDragSelectState({ scrollState })

    var playlistName by remember { mutableStateOf("") }
    var items by remember { mutableStateOf<List<DAudioPlaylistItem>>(listOf()) }
    var albumCovers by remember { mutableStateOf<List<PlaylistAlbumCover>>(emptyList()) }
    var version by remember { mutableIntStateOf(0) }
    var showMore by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }

    val reload: suspend () -> Unit = {
        val (name, list) = withIO {
            AudioPlaylistManager.playlist(playlistId)?.name to AudioPlaylistManager.playlistItemsPage(playlistId, 0, 1000)
        }
        playlistName = name ?: ""
        items = list
    }

    // Reload when coming back from add-items or after any mutation.
    LaunchedEffect(playlistId, version) {
        reload()
    }

    val topRefreshLayoutState = rememberRefreshLayoutState {
        scope.launch {
            reload()
            setRefreshState(RefreshContentState.Finished)
        }
    }

    // Album mosaic loads from the items alone; legacy rows backfill inside.
    LaunchedEffect(items) {
        albumCovers = if (items.isEmpty()) emptyList() else loadPlaylistAlbumCovers(items)
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) version++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Track rows always list A-Z; "custom" only survives as the play order.
    val sorted = items.sortedBy { it.title.lowercase() }
    val libraryByPath = remember(library) { library.associateBy { it.path } }
    // Resolve rows to the same DAudio shown in AudioListItem so selection
    // records one id space (library id, or audioPath for off-page rows).
    val rowAudios = sorted.map { libraryByPath[it.audioPath] ?: it.toDAudio() }
    val selectItems = remember(rowAudios) { rowAudios.map { IDData(it.id) } }
    val pathById = remember(library) { library.associateBy { it.id } }
    // Floating player bar clearance, measured live like on the other pages.
    val density = LocalDensity.current
    var playerBarClearance by remember { mutableStateOf(0.dp) }
    val contextActive = audioPlaylistVM.activePlaylistId.value == playlistId
    val isPlayingContext = contextActive && isPlaying

    PBackHandler(enabled = dragSelectState.selectMode) {
        dragSelectState.exitSelectMode()
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
                    withIO { AudioPlaylistManager.renamePlaylist(playlistId, name) }
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
                    withIO { AudioPlaylistManager.deletePlaylist(playlistId) }
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

    PScaffold(
        topBar = {
            PTopAppBar(
                title = "",
                navController = navController,
                actions = {
                    PIconButton(
                        icon = Res.drawable.more_horiz,
                        contentDescription = stringResource(Res.string.more),
                        click = { showMore = true },
                    )
                },
            )
        },
        bottomBar = {
            AnimatedBottomAction(visible = dragSelectState.showBottomActions()) {
                AudioFilesSelectModeBottomActions(
                    audioVM = audioVM,
                    audioPlaylistVM = audioPlaylistVM,
                    tagsVM = tagsVM,
                    tagsState = tagsState,
                    dragSelectState = dragSelectState,
                    removeFromPlaylist = {
                        scope.launch {
                            val paths = dragSelectState.selectedIds.map { pathById[it]?.path ?: it }
                            if (paths.isNotEmpty()) {
                                withIO { AudioPlaylistManager.removePlaylistItems(playlistId, paths) }
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
                // Two header items (hero, acts) precede the track rows.
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .listDragSelect(items = selectItems, state = dragSelectState, itemIndexOffset = 2),
                    state = scrollState,
                ) {
                    item(key = "hero") {
                        PlaylistHeaderRow(
                            name = playlistName,
                            itemCount = items.size,
                            albums = albumCovers,
                            gradientIndex = playlistId.hashCode(),
                        )
                    }
                    item(key = "acts") {
                        PlaylistActionsRow(
                            enabled = items.isNotEmpty(),
                            isPlaying = isPlayingContext,
                            onPlayAll = {
                                // Toggle pause/resume while this playlist is the
                                // active source; otherwise start it from the top.
                                if (contextActive) {
                                    if (isPlaying) audioPause() else audioPlay()
                                } else {
                                    scope.launch { playFrom(playlistId, null, audioPlaylistVM) }
                                }
                            },
                            onShuffle = { scope.launch { playShuffled(playlistId, audioPlaylistVM) } },
                        )
                    }
                    items(sorted.size, key = { sorted[it].id }) { index ->
                        val item = rowAudios[index]
                        AudioListItem(
                            item = item,
                            audioVM = audioVM,
                            audioPlaylistVM = audioPlaylistVM,
                            tagsVM = tagsVM,
                            castVM = castVM,
                            tags = emptyList(),
                            dragSelectState = dragSelectState,
                            isCurrentlyPlaying = isPlaying && audioPlaylistVM.selectedPath.value == item.path,
                            isInPlaylist = audioPlaylistVM.isInQueue(item.path),
                        )
                        VerticalSpace(8.dp)
                    }
                    item(key = "bottom") {
                        // Player bar clearance when playing; plain nav-inset
                        // spacing otherwise — never both stacked.
                        VerticalSpace(dp = maxOf(playerBarClearance, 40.dp + paddingValues.calculateBottomPadding()))
                    }
                }
            }
            AudioPlayerBar(
                audioPlaylistVM, castVM,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .onSizeChanged { playerBarClearance = with(density) { it.height.toDp() } },
                dragSelectState = dragSelectState,
            )
            AudioCastPlayerBar(castVM = castVM, modifier = Modifier.align(Alignment.BottomCenter), dragSelectState = dragSelectState)
        }
    }
}

/** Point the queue at this playlist starting from [startPath] (null = first). */
private suspend fun playFrom(playlistId: String, startPath: String?, playlistVM: AudioPlaylistViewModel) {
    val start = withIO { AudioQueueManager.setPlaylistSource(playlistId, startPath) }
    if (start != null) {
        audioJustPlayWithNotificationCheck(start)
        playlistVM.onStarted(start)
    }
}

/** Start the playlist from a shuffled pick instead of the top. */
private suspend fun playShuffled(playlistId: String, playlistVM: AudioPlaylistViewModel) {
    withIO { AudioQueueManager.setPlaylistSource(playlistId, null) }
    val next = withIO { AudioQueueManager.resolveNext(isNext = true, shuffle = true) }
    if (next != null) {
        audioJustPlayWithNotificationCheck(next)
        playlistVM.onStarted(next)
    }
}

private fun DAudioPlaylistItem.toDAudio(): DAudio = DAudio(
    id = audioPath,
    title = title,
    artist = artist,
    path = audioPath,
    duration = duration,
    size = 0,
    bucketId = "",
    albumId = "",
    createdAt = TimeHelper.now(),
    updatedAt = TimeHelper.now(),
)
