package com.ismartcoding.plain.ui.page.playlist

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.Modifier
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
import com.ismartcoding.plain.features.audio.AudioQueueManager
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.audioIsPlayingFlow
import com.ismartcoding.plain.platform.audioJustPlayWithNotificationCheck
import com.ismartcoding.plain.platform.audioPause
import com.ismartcoding.plain.platform.audioPlay
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.components.PlaylistNameDialog
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.models.AudioViewModel
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.models.TagsViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.ui.base.dragselect.rememberListDragSelectState
import com.ismartcoding.plain.ui.page.audio.components.AudioListItem
import com.ismartcoding.plain.ui.page.audio.components.ViewAudioBottomSheet
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
    var sort by remember { mutableStateOf(PlaylistSortOrder.CUSTOM) }
    var version by remember { mutableIntStateOf(0) }
    var showMore by remember { mutableStateOf(false) }
    var showRename by remember { mutableStateOf(false) }

    // Reload when coming back from add-items or after any mutation.
    LaunchedEffect(playlistId, version) {
        val (name, list) = withIO {
            AudioQueueManager.playlist(playlistId)?.name to AudioQueueManager.playlistItemsPage(playlistId, 0, 1000)
        }
        playlistName = name ?: ""
        items = list
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) version++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val sorted = items.sortedByOrder(sort)

    val libraryByPath = remember(library) { library.associateBy { it.path } }
    val contextActive = audioPlaylistVM.activePlaylistId.value == playlistId
    val isPlayingContext = contextActive && isPlaying

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
                    withIO { AudioQueueManager.renamePlaylist(playlistId, name) }
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
            sort = sort,
            onDismiss = { showMore = false },
            onRename = { showRename = true },
            onAddItems = { navController.navigate(Routing.PlaylistAddItems(playlistId)) },
            onToggleSort = { sort = sort.next() },
            onDelete = {
                scope.launch {
                    withIO { AudioQueueManager.deletePlaylist(playlistId) }
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
    )
    CastDialog(castVM)

    Column(Modifier.fillMaxSize()) {
        PTopAppBar(
            title = stringResource(Res.string.playlists),
            navController = navController,
            actions = {
                PIconButton(
                    icon = Res.drawable.more_horiz,
                    contentDescription = stringResource(Res.string.more),
                    click = { showMore = true },
                )
            },
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item(key = "hero") {
                PlaylistHeaderRow(
                    name = playlistName,
                    itemCount = items.size,
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
                val row = sorted[index]
                // Prefer the real library item (id/tags/cover); fall back to a
                // synthesized one for tracks outside the loaded library page.
                val item = libraryByPath[row.audioPath] ?: row.toDAudio()
                AudioListItem(
                    item = item,
                    audioVM = audioVM,
                    audioPlaylistVM = audioPlaylistVM,
                    tagsVM = tagsVM,
                    castVM = castVM,
                    tags = emptyList(),
                    dragSelectState = dragSelectState,
                    isCurrentlyPlaying = isPlaying && audioPlaylistVM.selectedPath.value == row.audioPath,
                    isInPlaylist = audioPlaylistVM.isInPlaylist(row.audioPath),
                )
                VerticalSpace(8.dp)
            }
            item(key = "bottom") { BottomSpace() }
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
