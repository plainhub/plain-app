package com.ismartcoding.plain.ui.page.audio.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.PSheetActionRow
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.abs

/** Soft cover gradients rotated by a stable index (light theme, e-ink friendly). */
private val AudioHomeGradients = listOf(
    listOf(Color(0xFFDDE2F9), Color(0xFFE8DEF8)),
    listOf(Color(0xFFE5F0FF), Color(0xFFDDE2F9)),
    listOf(Color(0xFFE8DEF8), Color(0xFFF2E7EF)),
    listOf(Color(0xFFDFF0E4), Color(0xFFDDE2F9)),
    listOf(Color(0xFFFFF0DC), Color(0xFFE8DEF8)),
    listOf(Color(0xFFDDE2F9), Color(0xFFDFF0E4)),
)

fun audioHomeGradientAt(index: Int): Brush {
    val colors = AudioHomeGradients[abs(index) % AudioHomeGradients.size]
    return Brush.linearGradient(colors)
}

/** Stable gradient index for a path so covers keep their color across screens. */
fun audioHomeGradientIndexOf(path: String): Int = abs(path.hashCode()) % AudioHomeGradients.size

@Composable
fun PlaylistCoverArtwork(gradientIndex: Int, modifier: Modifier = Modifier, iconSize: Int = 28) {
    Box(
        modifier = modifier.background(audioHomeGradientAt(gradientIndex)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(Res.drawable.music2),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier.size(iconSize.dp),
        )
    }
}

@Composable
fun ArtistAvatar(name: String, gradientIndex: Int, size: Int, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .size(size.dp)
            .clip(CircleShape)
            .background(audioHomeGradientAt(gradientIndex)),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = name.firstOrNull()?.uppercase() ?: "#",
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
        )
    }
}

/** Three animated bars marking the currently playing row. */
@Composable
fun PlayingEqIndicator(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "eq")
    val heights = listOf(6f, 12f, 8f).mapIndexed { i, h ->
        transition.animateFloat(
            initialValue = 0.4f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(500 + i * 120, easing = LinearEasing), RepeatMode.Reverse),
            label = "eq$i",
        ).value
    }
    Row(modifier = modifier, verticalAlignment = Alignment.Bottom) {
        heights.forEach { v ->
            Box(
                Modifier
                    .padding(horizontal = 1.dp)
                    .size(3.dp, (12 * v).dp)
                    .clip(RoundedCornerShape(1.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}

@Composable
fun HomeSongRow(
    item: DPlaylistAudio,
    isPlaying: Boolean,
    isCurrent: Boolean,
    onRowClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onRowClick)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(audioHomeGradientAt(audioHomeGradientIndexOf(item.path))),
            contentAlignment = Alignment.Center,
        ) {
            AudioCoverOrIcon(path = item.path)
        }
        Column(modifier = Modifier.weight(1f).padding(start = 12.dp, end = 8.dp)) {
            Text(
                text = item.title,
                style = MaterialTheme.typography.listItemTitle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = item.getSubtitle(),
                style = MaterialTheme.typography.listItemSubtitle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(top = 2.dp),
            )
        }
        if (isPlaying) {
            PlayingEqIndicator(modifier = Modifier.padding(end = 8.dp))
        }
        Icon(
            painter = painterResource(Res.drawable.more_three_dots),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .clickable(onClick = onMoreClick)
                .padding(8.dp),
        )
    }
}

/** Bottom-sheet actions for a track row: play / play next / add to playlist / add to queue. */
@Composable
fun SongMenuSheetContent(
    onPlay: () -> Unit,
    onPlayNext: () -> Unit,
    onAddToPlaylist: () -> Unit,
    onAddToQueue: () -> Unit,
) {
    PSheetActionRow(Res.drawable.play_arrow, stringResource(Res.string.play)) { onPlay() }
    PSheetActionRow(Res.drawable.skip_next, stringResource(Res.string.play_next)) { onPlayNext() }
    PSheetActionRow(Res.drawable.playlist_add, stringResource(Res.string.add_to_playlist)) { onAddToPlaylist() }
    PSheetActionRow(Res.drawable.list_music, stringResource(Res.string.add_to_queue)) { onAddToQueue() }
}
