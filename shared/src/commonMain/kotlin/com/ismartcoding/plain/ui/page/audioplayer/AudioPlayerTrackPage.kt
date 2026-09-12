package com.ismartcoding.plain.ui.page.audioplayer

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.audio.DPlaylistAudio
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.pause
import com.ismartcoding.plain.i18n.play_arrow
import com.ismartcoding.plain.lib.LrcParser
import com.ismartcoding.plain.platform.audioPause
import com.ismartcoding.plain.platform.audioPlay
import com.ismartcoding.plain.ui.page.audioplayer.components.AudioPlayerCover
import org.jetbrains.compose.resources.painterResource

internal enum class PlayerView { COVER, LYRICS }
private fun dpLerp(start: Dp, stop: Dp, fraction: Float): Dp = start + (stop - start) * fraction

/**
 * One pager page: a single track laid out as a cover view that continuously
 * morphs into an immersive lyrics view as [viewMode] changes. Owns the
 * per-track lyrics loading.
 */
@Composable
internal fun AudioPlayerTrackPage(
    item: DPlaylistAudio,
    progressMs: Long,
    isPlaying: Boolean,
    viewMode: PlayerView,
    onViewModeChange: (PlayerView) -> Unit,
    onSeek: (Long) -> Unit,
) {
    var lyrics by remember(item.path) { mutableStateOf<List<LrcParser.LrcLine>?>(null) }
    LaunchedEffect(item.path) {
        lyrics = loadLyrics(item.path)
    }
    val hasLyrics = !lyrics.isNullOrEmpty()

    // 0 = cover layout, 1 = immersive lyrics layout
    val p by animateFloatAsState(
        targetValue = if (viewMode == PlayerView.LYRICS) 1f else 0f,
        animationSpec = tween(350, easing = FastOutSlowInEasing),
        label = "lyricsProgress",
    )

    BoxWithConstraints(modifier = Modifier.fillMaxSize().padding(horizontal = 32.dp)) {
        val width = maxWidth
        val height = maxHeight
        val headerHeight = 64.dp
        // Reserve room for title/artist + 2-line lyrics
        // preview so the artwork sits optically centered.
        val reservedBottom = 152.dp
        val bigTop = ((height - width - reservedBottom) / 2).coerceAtLeast(0.dp)
        val coverSize = dpLerp(width, 48.dp, p)
        val coverTop = dpLerp(bigTop, 8.dp, p)

        // Cover: single instance animating from the
        // centered artwork to the small header corner.
        Box(
            modifier = Modifier
                .offset(y = coverTop)
                .size(coverSize)
                .clip(RoundedCornerShape((24f + (16f - 24f) * p).dp))
                .clickable {
                    onViewModeChange(if (viewMode == PlayerView.COVER) PlayerView.LYRICS else PlayerView.COVER)
                },
        ) {
            AudioPlayerCover(path = item.path, modifier = Modifier.fillMaxSize())
        }

        // Big title/artist tracks the cover and fades out;
        // a 2-line lyrics preview below doubles as the
        // entry into the lyrics page.
        Column(
            modifier = Modifier.fillMaxWidth().graphicsLayer { alpha = 1f - p },
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(modifier = Modifier.height(coverTop + coverSize + 16.dp))
            Text(
                text = item.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            Text(
                text = item.artist,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth(),
            )
            if (hasLyrics) {
                val parsed = lyrics.orEmpty()
                val firstIndex = parsed.indexOfLast { it.timeMs <= progressMs }.coerceAtLeast(0)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onViewModeChange(PlayerView.LYRICS) },
                ) {
                    Text(
                        text = parsed[firstIndex].text,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                    )
                    parsed.getOrNull(firstIndex + 1)?.let { next ->
                        Text(
                            text = next.text,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 4.dp),
                        )
                    }
                }
            }
        }

        // Small header next to the shrunken cover, with
        // an inline play/pause for reading along.
        Row(
            modifier = Modifier.fillMaxWidth().offset(y = 8.dp).height(48.dp).graphicsLayer { alpha = p },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Spacer(modifier = Modifier.width(64.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = item.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(
                onClick = { if (isPlaying) audioPause() else audioPlay() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
            ) {
                Icon(
                    painter = painterResource(if (isPlaying) Res.drawable.pause else Res.drawable.play_arrow),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp),
                )
            }
        }

        // Lyrics fill the whole page below the header.
        if (p > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = headerHeight)
                    .graphicsLayer { alpha = (p * 1.5f).coerceIn(0f, 1f) }
                    .then(if (p > 0.5f) Modifier.clickable { onViewModeChange(PlayerView.COVER) } else Modifier),
            ) {
                AudioPlayerLyrics(
                    lines = lyrics,
                    progressMs = progressMs,
                    onSeek = onSeek,
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}
