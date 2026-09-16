package com.ismartcoding.plain.ui.page.playlist

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavHostController
import com.ismartcoding.plain.db.DAudioPlaylistSong
import com.ismartcoding.plain.features.audio.AudioQueueManager
import com.ismartcoding.plain.features.audio.toPlaylistAudio
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.platform.audioIsPlayingFlow
import com.ismartcoding.plain.platform.audioJustPlayWithNotificationCheck
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.components.PlaylistNameDialog
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.audio.components.PlaylistCoverArtwork
import com.ismartcoding.plain.ui.theme.dialogSheetBackground
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource

private const val SORT_CUSTOM = 0
private const val SORT_TITLE_ASC = 1
private const val SORT_TITLE_DESC = 2
private val SORT_NAMES
    @Composable get() = listOf(
        stringResource(Res.string.sort_custom),
        stringResource(Res.string.sort_title_asc),
        stringResource(Res.string.sort_title_desc),
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistDetailPage(
    navController: NavHostController,
    playlistId: String,
    audioPlaylistVM: AudioPlaylistViewModel,
) {
    val scope = rememberCoroutineScope()
    val isAudioPlaying by audioIsPlayingFlow().collectAsState()

    var playlistName by remember { mutableStateOf("") }
    var songs by remember { mutableStateOf<List<DAudioPlaylistSong>>(listOf()) }
    var sort by remember { mutableIntStateOf(SORT_CUSTOM) }
    var version by remember { mutableIntStateOf(0) }
    var showMore by remember { mutableStateOf(false) }
    var menuSong by remember { mutableStateOf<DAudioPlaylistSong?>(null) }
    var showRename by remember { mutableStateOf(false) }
    var showDelete by remember { mutableStateOf(false) }

    // Reload when coming back from add-songs or after any mutation.
    LaunchedEffect(playlistId, version) {
        val (name, list) = withIO {
            AudioQueueManager.playlist(playlistId)?.name to AudioQueueManager.playlistSongsPage(playlistId, 0, 1000)
        }
        playlistName = name ?: ""
        songs = list
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) version++
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    val sorted = when (sort) {
        SORT_TITLE_ASC -> songs.sortedBy { it.title.lowercase() }
        SORT_TITLE_DESC -> songs.sortedByDescending { it.title.lowercase() }
        else -> songs
    }
    val totalMinutes = songs.sumOf { it.duration } / 60000

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
    if (showDelete) {
        androidx.compose.material3.AlertDialog(
            containerColor = MaterialTheme.colorScheme.dialogSheetBackground,
            onDismissRequest = { showDelete = false },
            title = { Text(stringResource(Res.string.delete_playlist)) },
            text = {
                Text(
                    LocaleHelper.getStringF(
                        Res.string.delete_playlist_confirm_text,
                        playlistName,
                        songs.size.toString(),
                    ),
                )
            },
            confirmButton = {
                PFilledButton(
                    text = stringResource(Res.string.delete),
                    type = com.ismartcoding.plain.enums.ButtonType.DANGER,
                    buttonSize = com.ismartcoding.plain.enums.ButtonSize.SMALL,
                    onClick = {
                        showDelete = false
                        scope.launch {
                            withIO { AudioQueueManager.deletePlaylist(playlistId) }
                            DialogHelper.showMessage(deletedMsg)
                            navController.popBackStack()
                        }
                    },
                )
            },
            dismissButton = { PTextButton(text = stringResource(Res.string.cancel), onClick = { showDelete = false }) },
        )
    }
    if (showMore) {
        PModalBottomSheet(onDismissRequest = { showMore = false }) {
            PBottomSheetTopAppBar(title = playlistName)
            PSheetActionRow(Res.drawable.pen, stringResource(Res.string.rename_playlist)) {
                showMore = false
                showRename = true
            }
            PSheetActionRow(Res.drawable.plus, stringResource(Res.string.add_songs)) {
                showMore = false
                navController.navigate(Routing.PlaylistAddSongs(playlistId))
            }
            PSheetActionRow(Res.drawable.sort, stringResource(Res.string.sort_order) + " · " + SORT_NAMES[sort]) {
                showMore = false
                sort = (sort + 1) % 3
            }
            PSheetActionRow(Res.drawable.delete_forever, stringResource(Res.string.delete_playlist)) {
                showMore = false
                showDelete = true
            }
            BottomSpace()
        }
    }
    if (menuSong != null) {
        val song = menuSong!!
        PModalBottomSheet(onDismissRequest = { menuSong = null }) {
            PBottomSheetTopAppBar(title = song.title)
            PSheetActionRow(Res.drawable.play_arrow, stringResource(Res.string.play)) {
                menuSong = null
                scope.launch {
                    val start = withIO { AudioQueueManager.setPlaylistSource(playlistId, song.audioPath) }
                    if (start != null) audioJustPlayWithNotificationCheck(start)
                }
            }
            PSheetActionRow(Res.drawable.skip_next, stringResource(Res.string.play_next)) {
                menuSong = null
                scope.launch {
                    withIO { AudioQueueManager.enqueue(listOf(song.toPlaylistAudio()), playNext = true) }
                    audioPlaylistVM.loadAsync()
                }
            }
            PSheetActionRow(Res.drawable.delete_forever, stringResource(Res.string.remove_from_playlist)) {
                menuSong = null
                scope.launch {
                    withIO { AudioQueueManager.removePlaylistSong(playlistId, song.audioPath) }
                    version++
                }
            }
            BottomSpace()
        }
    }

    Column(Modifier.fillMaxSize()) {
        PTopAppBar(
            title = stringResource(Res.string.playlists),
            navigationIcon = null,
            navController = navController,
            actions = {
                PIconButton(
                    icon = Res.drawable.more_three_dots,
                    contentDescription = stringResource(Res.string.more),
                    click = { showMore = true },
                )
            },
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item(key = "hero") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlaylistCoverArtwork(
                        gradientIndex = playlistId.hashCode(),
                        modifier = Modifier.size(96.dp).clip(RoundedCornerShape(12.dp)),
                        iconSize = 36,
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                        Text(
                            text = playlistName,
                            style = MaterialTheme.typography.listItemTitle(),
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = LocaleHelper.getStringF(Res.string.n_songs, songs.size) + " · " +
                                LocaleHelper.getStringF(Res.string.total_minutes, totalMinutes),
                            style = MaterialTheme.typography.listItemSubtitle(),
                            modifier = Modifier.padding(top = 4.dp),
                        )
                    }
                }
            }
            item(key = "acts") {
                Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp)) {
                    PFilledButton(
                        text = stringResource(Res.string.play_all),
                        icon = org.jetbrains.compose.resources.painterResource(Res.drawable.play_arrow),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            scope.launch {
                                val start = withIO { AudioQueueManager.setPlaylistSource(playlistId, null) }
                                if (start != null) audioJustPlayWithNotificationCheck(start)
                            }
                        },
                    )
                    Box(Modifier.width(12.dp))
                    POutlinedButton(
                        text = stringResource(Res.string.shuffle_play),
                        icon = org.jetbrains.compose.resources.painterResource(Res.drawable.shuffle),
                        modifier = Modifier.weight(1f),
                        onClick = {
                            scope.launch {
                                withIO { AudioQueueManager.setPlaylistSource(playlistId, null) }
                                val next = withIO { AudioQueueManager.resolveNext(isNext = true, shuffle = true) }
                                if (next != null) audioJustPlayWithNotificationCheck(next)
                            }
                        },
                    )
                }
            }
            items(sorted.size, key = { sorted[it].id }) { index ->
                val song = sorted[index]
                val isCurrent = audioPlaylistVM.selectedPath.value == song.audioPath
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            scope.launch {
                                val start = withIO { AudioQueueManager.setPlaylistSource(playlistId, song.audioPath) }
                                if (start != null) audioJustPlayWithNotificationCheck(start)
                            }
                        }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.width(32.dp)) {
                            Text(
                                text = "${index + 1}",
                                style = MaterialTheme.typography.listItemSubtitle(),
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(start = 8.dp),
                            )
                        }
                        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.listItemTitle(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = song.artist,
                                style = MaterialTheme.typography.listItemSubtitle(),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Text(
                            text = formatDurationLabel(song.duration),
                            style = MaterialTheme.typography.listItemSubtitle(),
                            modifier = Modifier.padding(end = 4.dp),
                        )
                        Icon(
                            painter = org.jetbrains.compose.resources.painterResource(Res.drawable.more_three_dots),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable { menuSong = song }
                                .padding(8.dp),
                        )
                    }
                }
            }
            item(key = "bottom") { BottomSpace() }
        }
    }
}

private fun formatDurationLabel(durationMs: Long): String {
    val totalSeconds = durationMs / 1000
    return "${totalSeconds / 60}:" + (totalSeconds % 60).toString().padStart(2, '0')
}
