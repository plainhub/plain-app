package com.ismartcoding.plain.ui.page.audioplayer.components

import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import com.ismartcoding.plain.ui.components.PulsatingWave

/**
 * Mini player bar: cover + title/artist + skip-next + play/pause + queue,
 * with a thin inset progress line along the bottom edge.
 */
@Composable
fun AudioPlayerBarCard(
    title: String,
    artist: String,
    coverPath: String?,
    progress: Float,
    duration: Float,
    isPlaying: Boolean,
    onClickContent: () -> Unit,
    onClickSkipNext: () -> Unit,
    onPlayPause: () -> Unit,
    onClickQueue: () -> Unit,
) {
    Column(
        modifier = Modifier
            .navigationBarsPadding()
            .padding(start = 12.dp, end = 12.dp, bottom = 10.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClickContent),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 10.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(MaterialTheme.colorScheme.surfaceContainerHigh, MaterialTheme.colorScheme.tertiaryContainer),
                        ),
                    ),
                contentAlignment = Alignment.Center,
            ) {
                if (coverPath != null) {
                    com.ismartcoding.plain.ui.page.audio.components.AudioCoverOrIcon(path = coverPath)
                } else {
                    Icon(
                        painter = painterResource(Res.drawable.music2),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(22.dp),
                    )
                }
                if (isPlaying) {
                    PulsatingWave(
                        isPlaying = true,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(3.dp),
                    )
                }
            }
            Column(modifier = Modifier.weight(1f).padding(start = 10.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.listItemTitle(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                )
                Text(
                    text = artist,
                    style = MaterialTheme.typography.listItemSubtitle(),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .basicMarquee(iterations = Int.MAX_VALUE)
                        .padding(top = 1.dp),
                )
            }
            IconButton(onClick = onClickSkipNext, modifier = Modifier.size(36.dp)) {
                Icon(
                    painter = painterResource(Res.drawable.skip_next),
                    contentDescription = stringResource(Res.string.play_next),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(22.dp),
                )
            }
            IconButton(
                onClick = onPlayPause,
                modifier = Modifier
                    .padding(start = 4.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer),
            ) {
                Icon(
                    painter = painterResource(if (isPlaying) Res.drawable.pause else Res.drawable.play_arrow),
                    contentDescription = if (isPlaying) stringResource(Res.string.pause) else stringResource(Res.string.play),
                    tint = if (isPlaying) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer,
                    modifier = Modifier.size(20.dp),
                )
            }
            IconButton(onClick = onClickQueue, modifier = Modifier.size(36.dp)) {
                Icon(
                    painter = painterResource(Res.drawable.list_music),
                    contentDescription = stringResource(Res.string.playlist),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp),
                )
            }
        }
        Box(
            modifier = Modifier
                .padding(start = 14.dp, end = 14.dp, bottom = 6.dp)
                .fillMaxWidth()
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MaterialTheme.colorScheme.outlineVariant),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(if (duration == 0f) 0f else (progress / duration).coerceIn(0f, 1f))
                    .height(3.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(MaterialTheme.colorScheme.primary),
            )
        }
    }
}
