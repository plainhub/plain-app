package com.ismartcoding.plain.ui.page.cast

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.data.IMedia
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.reorderable.ReorderableCollectionItemScope
import com.ismartcoding.plain.ui.components.PulsatingWave
import com.ismartcoding.plain.ui.theme.circleBackground
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import com.ismartcoding.plain.ui.theme.red
import com.ismartcoding.plain.ui.theme.secondaryTextColor

@Composable
internal fun ReorderableCollectionItemScope.AudioCastPlaylistItemContent(
    item: IMedia, index: Int, isPlaying: Boolean, isLoading: Boolean, onRemove: () -> Unit,
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }

    LaunchedEffect(item.path) {
        val audio = withIO { DPlaylistAudio.fromPath(context, item.path) }
        title = audio.title; artist = audio.artist
    }

    Row(modifier = Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(36.dp).clip(CircleShape)
            .background(if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.circleBackground)
            .draggableHandle(), contentAlignment = Alignment.Center) {
            when {
                isLoading -> CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary, strokeWidth = 2.dp)
                isPlaying -> PulsatingWave(isPlaying = true, color = MaterialTheme.colorScheme.onPrimary, modifier = Modifier.align(Alignment.Center))
                else -> Text(text = "${index + 1}", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.secondaryTextColor)
            }
        }
        HorizontalSpace(16.dp)
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(text = title, style = MaterialTheme.typography.listItemTitle())
            VerticalSpace(4.dp)
            Text(text = artist, style = MaterialTheme.typography.listItemSubtitle())
        }
        PIconButton(icon = R.drawable.playlist_remove, tint = MaterialTheme.colorScheme.red,
            contentDescription = stringResource(R.string.remove_from_cast_queue), click = onRemove)
    }
}
