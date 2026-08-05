package com.ismartcoding.plain.ui.page.cast

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.ismartcoding.plain.helpers.withIO
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.formatDuration
import com.ismartcoding.plain.lib.extensions.getFilenameWithoutExtensionFromPath
import com.ismartcoding.plain.lib.extensions.isAudioFast
import com.ismartcoding.plain.lib.extensions.isImageFast
import com.ismartcoding.plain.lib.extensions.isVideoFast
import com.ismartcoding.plain.platform.getAudioMetadata
import com.ismartcoding.plain.ui.base.PlayerSlider
import com.ismartcoding.plain.ui.base.PlayerSliderDefaults
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.theme.cardBackgroundNormal
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.secondaryTextColor
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

@Composable
fun CastNowPlayingSection(
    currentUri: String,
    isPlaying: Boolean,
    progress: Float,
    duration: Float,
    supportsCallback: Boolean,
    isLoading: Boolean,
    onPlay: () -> Unit,
    onPause: () -> Unit,
    onSeek: (Float) -> Unit,
) {
    val isVideo = currentUri.isVideoFast()
    val isAudio = currentUri.isAudioFast()
    val isImage = currentUri.isImageFast()
    val canControl = isVideo || isAudio

    var title by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }

    LaunchedEffect(currentUri) {
        if (currentUri.isEmpty()) {
            title = ""; subtitle = ""
        } else if (isAudio) {
            val (t, a) = withIO { getAudioMetadata(currentUri) }
            title = t; subtitle = a
        } else {
            title = currentUri.getFilenameWithoutExtensionFromPath()
            subtitle = ""
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.cardBackgroundNormal),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isImage -> AsyncImage(
                    model = currentUri,
                    contentDescription = title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxWidth().aspectRatio(1f),
                )
                isVideo -> AsyncImage(
                    model = currentUri,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                )
                isAudio -> AudioCoverArt(path = currentUri, modifier = Modifier.fillMaxWidth().aspectRatio(1f))
                else -> Box(
                    modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(Res.drawable.cast),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondaryTextColor,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(36.dp),
                    strokeWidth = 3.dp,
                    color = Color.White,
                )
            }
        }

        VerticalSpace(16.dp)
        Text(
            text = title.ifEmpty { stringResource(Res.string.casting) },
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        if (subtitle.isNotEmpty()) {
            VerticalSpace(4.dp)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.listItemSubtitle(),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
        }

        if (canControl) {
            if (supportsCallback && duration > 0f) {
                VerticalSpace(16.dp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text(
                        modifier = Modifier.width(52.dp),
                        text = progress.toLong().formatDuration(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                    Box(modifier = Modifier.weight(1f).padding(horizontal = 4.dp)) {
                        PlayerSlider(
                            modifier = Modifier.fillMaxWidth().height(20.dp),
                            progress = if (duration > 0f) (progress / duration).coerceIn(0f, 1f) else 0f,
                            bufferedProgress = 0f,
                            onProgressChange = { ratio -> onSeek(ratio * duration) },
                            colors = PlayerSliderDefaults.lightColors(),
                        )
                    }
                    Text(
                        modifier = Modifier.width(52.dp),
                        text = duration.toLong().formatDuration(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            VerticalSpace(12.dp)
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.primary)
                    .clickable { if (isPlaying) onPause() else onPlay() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(if (isPlaying) Res.drawable.pause else Res.drawable.play_arrow),
                    contentDescription = if (isPlaying) stringResource(Res.string.pause) else stringResource(Res.string.play),
                    tint = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}
