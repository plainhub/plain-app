package com.ismartcoding.plain.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.db.DAudioPlaylist
import com.ismartcoding.plain.features.audio.AudioQueueManager
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.coIO
import com.ismartcoding.plain.ui.base.BottomSpace
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.page.audio.components.PlaylistCoverArtwork
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.stringResource
import kotlinx.coroutines.CoroutineScope
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle

/**
 * Pick a playlist for [item] ("添加到歌单"), with an inline create flow: the
 * new playlist is created and the track added to it in one step.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddToPlaylistSheet(
    item: DPlaylistAudio,
    onDismiss: () -> Unit,
) {
    val scope = rememberCoroutineScope()
    var playlists by remember { mutableStateOf<List<Pair<DAudioPlaylist, Int>>>(listOf()) }
    var showCreate by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        coIO { playlists = AudioQueueManager.playlists() }
    }

    PModalBottomSheet(onDismissRequest = onDismiss) {
        PBottomSheetTopAppBar(title = stringResource(Res.string.add_to_playlist))
        Column {
            playlists.forEach { (pl, count) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            onDismiss()
                            scope.launch {
                                coIO { AudioQueueManager.addPlaylistSongs(pl.id, listOf(item)) }
                                toastPlaylistAdded()
                            }
                        }
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    PlaylistCoverArtwork(
                        gradientIndex = pl.id.hashCode(),
                        modifier = Modifier.size(44.dp).clip(RoundedCornerShape(8.dp)),
                        iconSize = 20,
                    )
                    Column(modifier = Modifier.weight(1f).padding(start = 12.dp)) {
                        Text(text = pl.name, style = MaterialTheme.typography.listItemTitle(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(
                            text = com.ismartcoding.plain.platform.LocaleHelper.getStringF(Res.string.n_songs, count),
                            style = MaterialTheme.typography.listItemSubtitle(),
                        )
                    }
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { showCreate = true }
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.plus),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Text(
                    text = stringResource(Res.string.new_playlist),
                    style = MaterialTheme.typography.listItemTitle(),
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 12.dp),
                )
            }
            BottomSpace()
        }
    }

    if (showCreate) {
        PlaylistNameDialog(
            title = stringResource(Res.string.new_playlist),
            initial = "",
            confirmText = stringResource(Res.string.create),
            onConfirm = { name ->
                showCreate = false
                onDismiss()
                scope.launch {
                    coIO {
                        val pl = AudioQueueManager.createPlaylist(name)
                        AudioQueueManager.addPlaylistSongs(pl.id, listOf(item))
                        toastPlaylistAdded()
                    }
                }
            },
            onDismiss = { showCreate = false },
        )
    }
}

private fun toastPlaylistAdded() {
    com.ismartcoding.plain.lib.coMain {
        com.ismartcoding.plain.ui.helpers.DialogHelper.showMessage(
            com.ismartcoding.plain.platform.LocaleHelper.getStringAsync(Res.string.added_to_playlist),
        )
    }
}
