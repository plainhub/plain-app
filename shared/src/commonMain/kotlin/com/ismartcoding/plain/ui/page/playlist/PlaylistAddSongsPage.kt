package com.ismartcoding.plain.ui.page.playlist

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import org.jetbrains.compose.resources.stringResource
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.ismartcoding.plain.audio.DAudio
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.audio.AudioQueueManager
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.formatDuration
import com.ismartcoding.plain.lib.withIO
import com.ismartcoding.plain.platform.LocaleHelper
import com.ismartcoding.plain.preferences.AudioSortByPreference
import com.ismartcoding.plain.platform.searchMedia
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PTextField
import com.ismartcoding.plain.ui.base.PTopAppBar
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.components.CheckCircle
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.page.audio.components.PlaylistCoverArtwork
import com.ismartcoding.plain.ui.page.audio.components.audioHomeGradientIndexOf
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import kotlinx.coroutines.launch

/**
 * Multi-select library picker for adding tracks to a playlist. Tracks already
 * in the playlist start checked; unchecking them removes them on confirm.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlaylistAddSongsPage(
    navController: NavHostController,
    playlistId: String,
) {
    val scope = rememberCoroutineScope()
    var query by remember { mutableStateOf("") }
    var songs by remember { mutableStateOf<List<DAudio>>(listOf()) }
    var existing by remember { mutableStateOf<Set<String>>(emptySet()) }
    var selected by remember { mutableStateOf<Set<String>>(emptySet()) }

    LaunchedEffect(playlistId) {
        val paths = withIO { AudioQueueManager.playlistSongsPage(playlistId, 0, 5000).map { it.audioPath } }
        existing = paths.toSet()
        selected = paths.toSet()
    }
    LaunchedEffect(query) {
        songs = withIO {
            searchMedia(DataType.AUDIO, query, 500, 0, AudioSortByPreference.getValueAsync())
        }.filterIsInstance<DAudio>()
    }

    Column(Modifier.fillMaxSize()) {
        PTopAppBar(
            title = stringResource(Res.string.add_songs),
            navController = navController,
        )
        Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)) {
            PTextField(
                readOnly = false,
                value = query,
                onValueChange = { query = it },
                placeholder = stringResource(Res.string.search),
            )
        }
        Text(
            text = LocaleHelper.getStringF(Res.string.n_songs, songs.size),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp),
        )
        LazyColumn(modifier = Modifier.weight(1f)) {
            items(songs.size, key = { songs[it].path }) { index ->
                val song = songs[index]
                val isSelected = song.path in selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            selected = if (isSelected) selected - song.path else selected + song.path
                        }
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlaylistCoverArtwork(
                        gradientIndex = audioHomeGradientIndexOf(song.path),
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        iconSize = 20,
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(start = 12.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
                    ) {
                        Text(
                            text = song.title,
                            style = MaterialTheme.typography.listItemTitle(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        Text(
                            text = "${song.artist} · ${song.duration.formatDuration()}",
                            style = MaterialTheme.typography.listItemSubtitle(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 2.dp),
                        )
                    }
                    CheckCircle(
                        selected = isSelected,
                        modifier = Modifier.padding(end = 8.dp),
                        onClick = {
                            selected = if (isSelected) selected - song.path else selected + song.path
                        },
                    )
                }
            }
            item { VerticalSpace(24.dp) }
        }
        PFilledButton(
            text = LocaleHelper.getStringF(Res.string.add_n_songs, selected.size),
            enabled = selected.isNotEmpty(),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            onClick = {
                scope.launch {
                    val toAdd = selected - existing
                    val toRemove = existing - selected
                    val addedCount = withIO {
                        val audioByPath = songs.associateBy { it.path }
                        val items = toAdd.mapNotNull { path -> audioByPath[path]?.toPlaylistAudio() }
                        AudioQueueManager.addPlaylistSongs(playlistId, items)
                        toRemove.forEach { AudioQueueManager.removePlaylistSong(playlistId, it) }
                        items.size
                    }
                    DialogHelper.showMessage(LocaleHelper.getStringF(Res.string.added_n_songs, addedCount))
                    navController.popBackStack()
                }
            },
        )
    }
}
