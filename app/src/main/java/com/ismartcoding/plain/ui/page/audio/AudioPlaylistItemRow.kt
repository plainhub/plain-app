package com.ismartcoding.plain.ui.page.audio

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.R
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.audio.AudioPlayer
import com.ismartcoding.plain.features.Permissions
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.reorderable.ReorderableCollectionItemScope
import com.ismartcoding.plain.ui.components.PulsatingWave
import com.ismartcoding.plain.ui.models.AudioPlaylistViewModel
import com.ismartcoding.plain.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun ReorderableCollectionItemScope.AudioPlaylistItemRow(
    audio: DPlaylistAudio,
    index: Int,
    isPlaying: Boolean,
    audioPlaylistVM: AudioPlaylistViewModel,
    scope: CoroutineScope,
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(12.dp))
            .clickable { Permissions.checkNotification(context, R.string.audio_notification_prompt) { AudioPlayer.justPlay(context, audio) } },
        colors = CardDefaults.cardColors(containerColor = if (isPlaying) MaterialTheme.colorScheme.cardBackgroundActive else MaterialTheme.colorScheme.cardBackgroundNormal),
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier.size(36.dp).clip(CircleShape)
                    .background(if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.circleBackground)
                    .draggableHandle(),
                contentAlignment = Alignment.Center
            ) {
                if (isPlaying) {
                    PulsatingWave(isPlaying = true, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.align(Alignment.Center))
                } else {
                    Text(text = "${index + 1}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondaryTextColor)
                }
            }
            HorizontalSpace(16.dp)
            Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                Text(text = audio.title, style = MaterialTheme.typography.listItemTitle())
                VerticalSpace(4.dp)
                Text(text = audio.artist, style = MaterialTheme.typography.listItemSubtitle())
            }
            PIconButton(icon = R.drawable.playlist_remove, tint = MaterialTheme.colorScheme.red,
                contentDescription = stringResource(R.string.remove_from_playlist),
                click = { scope.launch(Dispatchers.IO) { audioPlaylistVM.removeAsync(context, audio.path) } })
        }
    }
}
