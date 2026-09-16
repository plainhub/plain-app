package com.ismartcoding.plain.ui.page.audio

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.audio.AudioQueueManager
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.formatDuration
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.audioIsPlayingFlow
import com.ismartcoding.plain.platform.audioJustPlayWithNotificationCheck
import com.ismartcoding.plain.preferences.AudioSortByPreference
import com.ismartcoding.plain.platform.searchMedia
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.POutlinedButton
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.nav.Routing
import com.ismartcoding.plain.ui.page.audio.components.ArtistAvatar
import com.ismartcoding.plain.ui.page.audio.components.SongMenuSheetContent
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

/** Artist detail: hero header plus the artist's tracks, play-all / shuffle. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AudioArtistPage(
    navController: NavHostController,
    artistName: String,
    audioPlaylistVM: AudioPlaylistViewModel,
) {
    val scope = rememberCoroutineScope()
    val isAudioPlaying by audioIsPlayingFlow().collectAsState()
    var songs by remember { mutableStateOf<List<DAudio>>(listOf()) }
    var menuSong by remember { mutableStateOf<com.ismartcoding.plain.audio.DPlaylistAudio?>(null) }
    var addToPlaylistAudio by remember { mutableStateOf<com.ismartcoding.plain.audio.DPlaylistAudio?>(null) }

    LaunchedEffect(artistName) {
        songs = withIO {
            searchMedia(DataType.AUDIO, artistName, 500, 0, AudioSortByPreference.getValueAsync())
        }.filterIsInstance<DAudio>().filter { it.artist == artistName }
    }

    val playAll: (Boolean) -> Unit = { shuffle ->
        scope.launch {
            val list = if (shuffle) songs.shuffled() else songs
            withIO {
                AudioQueueManager.clearQueue()
                AudioQueueManager.enqueue(list.map { it.toPlaylistAudio() })
            }
            val first = list.firstOrNull()?.toPlaylistAudio() ?: return@launch
            audioJustPlayWithNotificationCheck(first)
            audioPlaylistVM.loadAsync()
        }
    }

    Column(Modifier.fillMaxSize()) {
        PTopAppBar(
            title = stringResource(Res.string.artists),
            navController = navController,
        )
        LazyColumn(modifier = Modifier.fillMaxSize()) {
            item(key = "hero") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    ArtistAvatar(name = artistName, gradientIndex = artistName.hashCode(), size = 96)
                    Column(modifier = Modifier.weight(1f).padding(start = 16.dp)) {
                        Text(
                            text = artistName,
                            style = MaterialTheme.typography.listItemTitle(),
                            maxLines = 2,
                        )
                        Text(
                            text = com.ismartcoding.plain.platform.LocaleHelper.getStringF(
                                Res.string.n_songs,
                                songs.size,
                            ) + " · " + com.ismartcoding.plain.platform.LocaleHelper.getStringF(
                                Res.string.total_minutes,
                                songs.sumOf { it.duration } / 60000,
                            ),
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
                        icon = painterResource(Res.drawable.play_arrow),
                        modifier = Modifier.weight(1f),
                        onClick = { playAll(false) },
                    )
                    Box(Modifier.size(12.dp))
                    POutlinedButton(
                        text = stringResource(Res.string.shuffle_play),
                        icon = painterResource(Res.drawable.shuffle),
                        modifier = Modifier.weight(1f),
                        onClick = { playAll(true) },
                    )
                }
            }
            items(songs.size, key = { songs[it].path }) { index ->
                val song = songs[index]
                val plAudio = song.toPlaylistAudio()
                val isCurrent = audioPlaylistVM.selectedPath.value == song.path
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { scope.launch { audioPlaylistVM.playItem(plAudio) } }
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "${index + 1}",
                            style = MaterialTheme.typography.listItemSubtitle(),
                            color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 8.dp, end = 4.dp),
                        )
                        Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                            Text(
                                text = song.title,
                                style = MaterialTheme.typography.listItemTitle(),
                                maxLines = 1,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${song.artist} · ${song.duration.formatDuration()}",
                                style = MaterialTheme.typography.listItemSubtitle(),
                                maxLines = 1,
                            )
                        }
                        PIconButton(
                            icon = com.ismartcoding.plain.i18n.Res.drawable.more_three_dots,
                            contentDescription = null,
                            click = { menuSong = plAudio },
                        )
                    }
                }
            }
            item(key = "bottom") { BottomSpace() }
        }
    }

    if (menuSong != null) {
        val item = menuSong!!
        com.ismartcoding.plain.ui.base.PModalBottomSheet(onDismissRequest = { menuSong = null }) {
            com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar(title = item.title)
            SongMenuSheetContent(
                onPlay = {
                    menuSong = null
                    scope.launch { audioPlaylistVM.playItem(item) }
                },
                onPlayNext = {
                    menuSong = null
                    scope.launch {
                        withIO { AudioQueueManager.enqueue(listOf(item), playNext = true) }
                        audioPlaylistVM.loadAsync()
                    }
                },
                onAddToPlaylist = {
                    menuSong = null
                    addToPlaylistAudio = item
                },
                onAddToQueue = {
                    menuSong = null
                    scope.launch {
                        withIO { AudioQueueManager.enqueue(listOf(item)) }
                        audioPlaylistVM.loadAsync()
                    }
                },
            )
            BottomSpace()
        }
    }
    if (addToPlaylistAudio != null) {
        com.ismartcoding.plain.ui.components.AddToPlaylistSheet(
            item = addToPlaylistAudio!!,
            onDismiss = { addToPlaylistAudio = null },
        )
    }
}
