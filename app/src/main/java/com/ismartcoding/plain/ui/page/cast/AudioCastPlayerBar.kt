package com.ismartcoding.plain.ui.page.cast

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ismartcoding.lib.extensions.isGestureInteractionMode
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.DPlaylistAudio
import com.ismartcoding.plain.features.AudioPlayer
import com.ismartcoding.plain.features.media.CastPlayer
import com.ismartcoding.plain.ui.base.HorizontalSpace
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.base.dragselect.DragSelectState
import com.ismartcoding.plain.ui.models.CastViewModel
import com.ismartcoding.plain.ui.theme.PlainTheme
import com.ismartcoding.plain.ui.theme.listItemSubtitle
import com.ismartcoding.plain.ui.theme.listItemTitle
import kotlinx.coroutines.launch

@Composable
fun AudioCastPlayerBar(
    castVM: CastViewModel,
    modifier: Modifier = Modifier,
    dragSelectState: DragSelectState
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var title by remember { mutableStateOf("") }
    var artist by remember { mutableStateOf("") }
    var showCastPlaylist by remember { mutableStateOf(false) }
    val isPlaying by CastPlayer.isPlaying.collectAsState()
    val progress by CastPlayer.progress.collectAsState()
    val duration by CastPlayer.duration.collectAsState()
    val supportsCallback by CastPlayer.supportsCallback.collectAsState()

    val currentUri by CastPlayer.currentUri.collectAsState()

    LaunchedEffect(currentUri) {
        scope.launch {
            if (currentUri.isNotEmpty()) {
                val audio = withIO { DPlaylistAudio.fromPath(context, currentUri) }
                title = audio.title
                artist = audio.artist
            } else {
                title = ""
                artist = ""
            }
        }
    }

    AnimatedVisibility(
        visible = castVM.castMode.value && CastPlayer.currentDevice != null && !dragSelectState.selectMode,
        enter = slideInVertically { it },
        exit = slideOutVertically { it },
        modifier = modifier
    ) {
        val isGestureMode = context.isGestureInteractionMode()

        ElevatedCard(
            modifier = Modifier
                .padding(
                    start = 12.dp,
                    end = 12.dp,
                    bottom = if (isGestureMode) 16.dp else 8.dp
                )
                .fillMaxWidth(),
            shape = RoundedCornerShape(PlainTheme.CARD_RADIUS),
            elevation = CardDefaults.elevatedCardElevation(
                defaultElevation = 6.dp
            ),
            colors = CardDefaults.elevatedCardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            )
        ) {
            Column(modifier = Modifier.fillMaxWidth()) {
                LinearProgressIndicator(
                    progress = { 
                        if (supportsCallback && duration > 0f) {
                            progress / duration
                        } else {
                            0f
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, top = 4.dp, bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .weight(1f)
                            .clickable {
                                showCastPlaylist = true
                            }
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (title.isNotEmpty()) title else stringResource(R.string.casting),
                            style = MaterialTheme.typography.listItemTitle(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        VerticalSpace(4.dp)
                        Text(
                            text = if (artist.isNotEmpty()) artist else (CastPlayer.currentDevice?.description?.device?.friendlyName ?: ""),
                            style = MaterialTheme.typography.listItemSubtitle(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (currentUri.isNotEmpty()) {

                        IconButton(
                            onClick = {
                                if (isPlaying) {
                                    castVM.pauseCast()
                                } else {
                                    castVM.playCast()
                                }
                            },
                            modifier = Modifier
                                .size(48.dp)
                                .shadow(2.dp, CircleShape)
                                .clip(CircleShape)
                                .background(
                                    if (isPlaying)
                                        MaterialTheme.colorScheme.primaryContainer
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                        ) {
                            Icon(
                                painter = painterResource(id = if (isPlaying) R.drawable.pause else R.drawable.play_arrow),
                                contentDescription = if (isPlaying) stringResource(R.string.pause) else stringResource(R.string.play),
                                tint = if (isPlaying)
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                else
                                    MaterialTheme.colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        HorizontalSpace(8.dp)
                    }
                    IconButton(
                        onClick = { showCastPlaylist = true },
                        modifier = Modifier
                            .size(42.dp)
                            .clip(CircleShape)
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.list_music),
                            contentDescription = stringResource(R.string.playlist),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }

    if (showCastPlaylist) {
        AudioCastPlaylistPage(
            castVM = castVM,
            onDismissRequest = { showCastPlaylist = false }
        )
    }
} 