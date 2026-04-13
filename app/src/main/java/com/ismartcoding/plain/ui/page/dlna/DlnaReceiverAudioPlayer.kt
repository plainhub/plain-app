package com.ismartcoding.plain.ui.page.dlna

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.dlna.DlnaPlaybackState
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.ui.components.mediaviewer.video.findActivity
import com.ismartcoding.plain.ui.components.mediaviewer.video.rememberVideoPlayer
import com.ismartcoding.plain.ui.components.mediaviewer.video.setFullScreen
import com.ismartcoding.plain.ui.models.DlnaReceiverViewModel

@OptIn(UnstableApi::class)
@Composable
fun DlnaReceiverAudioPlayer(vm: DlnaReceiverViewModel, onExit: () -> Unit) {
    val context = LocalContext.current
    val mediaUri by DlnaRendererState.mediaUri.collectAsState()
    val mediaTitle by DlnaRendererState.mediaTitle.collectAsState()
    val albumArtUri by DlnaRendererState.mediaAlbumArtUri.collectAsState()
    var albumArtFailed by remember(albumArtUri) { mutableStateOf(false) }
    val playbackState by DlnaRendererState.playbackState.collectAsState()
    val seekTargetMs by DlnaRendererState.seekTargetMs.collectAsState()
    val positionMs by DlnaRendererState.currentPositionMs.collectAsState()
    val durationMs by DlnaRendererState.durationMs.collectAsState()

    val player = rememberVideoPlayer(context)

    LaunchedEffect(mediaUri) {
        if (mediaUri.isNotEmpty()) {
            player.playWhenReady = false
            player.setMediaItem(MediaItem.fromUri(mediaUri))
            player.prepare()
        }
    }
    LaunchedEffect(playbackState) {
        when (playbackState) {
            DlnaPlaybackState.PLAYING -> player.play()
            DlnaPlaybackState.PAUSED -> player.pause()
            DlnaPlaybackState.STOPPED -> { player.pause(); player.seekTo(0) }
            else -> {}
        }
    }
    LaunchedEffect(seekTargetMs) { seekTargetMs?.let { player.seekTo(it) } }
    LaunchedEffect(player) { vm.startPositionSync(player) }
    DisposableEffect(Unit) {
        context.findActivity().setFullScreen(true)
        onDispose { context.findActivity().setFullScreen(false); player.stop(); player.release() }
    }

    val gradient = Brush.verticalGradient(listOf(Color(0xFF1A1A2E), Color(0xFF16213E), Color(0xFF0F3460)))
    Box(
        modifier = Modifier.fillMaxSize().background(gradient),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier = Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onExit) {
                    Icon(
                        painter = painterResource(R.drawable.arrow_left),
                        contentDescription = stringResource(R.string.dlna_receiver_exit_player),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            Surface(
                modifier = Modifier.size(220.dp).clip(RoundedCornerShape(24.dp)),
                color = Color.White.copy(alpha = 0.08f),
                shape = RoundedCornerShape(24.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (albumArtUri.isNotEmpty() && !albumArtFailed) {
                        AsyncImage(
                            model = ImageRequest.Builder(context).data(albumArtUri).build(),
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize(),
                            onError = { albumArtFailed = true },
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.music2),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(96.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(36.dp))
            Text(
                text = mediaTitle.ifEmpty { stringResource(R.string.unknown) },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.weight(1f))
            AudioPlayerControls(
                positionMs = positionMs,
                durationMs = durationMs,
                isPlaying = playbackState == DlnaPlaybackState.PLAYING,
                onPlayPause = {
                    if (player.isPlaying) {
                        player.pause()
                        DlnaRendererState.playbackState.value = DlnaPlaybackState.PAUSED
                    } else {
                        player.play()
                        DlnaRendererState.playbackState.value = DlnaPlaybackState.PLAYING
                    }
                },
                onSeek = { ratio -> player.seekTo((ratio * durationMs).toLong()) },
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}


