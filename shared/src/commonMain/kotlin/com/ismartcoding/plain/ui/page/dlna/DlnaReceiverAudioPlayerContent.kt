package com.ismartcoding.plain.ui.page.dlna

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.compose.LocalPlatformContext
import coil3.request.ImageRequest
import com.ismartcoding.plain.features.dlna.DlnaPlaybackState
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.exitImmersiveFullscreen
import com.ismartcoding.plain.platform.rememberVideoPlayerController
import com.ismartcoding.plain.platform.setImmersiveFullscreen
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoState
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoPlayerEvent
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

@Composable
fun DlnaReceiverAudioPlayerContent(onExit: () -> Unit) {
    val mediaUri by DlnaRendererState.mediaUri.collectAsState()
    val mediaTitle by DlnaRendererState.mediaTitle.collectAsState()
    val albumArtUri by DlnaRendererState.mediaAlbumArtUri.collectAsState()
    var albumArtFailed by remember(albumArtUri) { mutableStateOf(false) }
    val remotePlaybackState by DlnaRendererState.playbackState.collectAsState()
    val seekTargetMs by DlnaRendererState.seekTargetMs.collectAsState()

    val controller = rememberVideoPlayerController(claimAudioSession = false)
    val state = remember { VideoState() }
    val context = LocalPlatformContext.current

    LaunchedEffect(controller) {
        state.initData(controller)
        controller.setEventListener { event ->
            when (event) {
                is VideoPlayerEvent.StateChanged -> {
                    if (event.duration > 0L) {
                        state.totalTime = event.duration
                    }
                    state.isPlaying = event.isPlaying
                    state.updateTime()
                }
                VideoPlayerEvent.PositionDiscontinuity -> {
                    state.isSeeking = false
                }
                VideoPlayerEvent.FirstFrameRendered -> {}
            }
        }
    }
    LaunchedEffect(mediaUri) {
        if (mediaUri.isNotEmpty()) {
            controller.pause()
            controller.setMediaItem(mediaUri)
            controller.prepare()
        }
    }
    LaunchedEffect(remotePlaybackState) {
        when (remotePlaybackState) {
            DlnaPlaybackState.PLAYING -> controller.play()
            DlnaPlaybackState.PAUSED -> controller.pause()
            DlnaPlaybackState.STOPPED -> controller.stopAndRewind()
            else -> {}
        }
    }
    LaunchedEffect(seekTargetMs) {
        seekTargetMs?.let { state.seekTo(it) }
    }
    LaunchedEffect(Unit) {
        while (true) {
            state.updateTime()
            DlnaRendererState.currentPositionMs.value = state.currentTime
            DlnaRendererState.durationMs.value = state.totalTime
            delay(1.seconds)
        }
    }
    setImmersiveFullscreen()
    DisposableEffect(Unit) {
        onDispose {
            exitImmersiveFullscreen()
            controller.release()
        }
    }

    val onPlayPause = {
        val wasPlaying = state.isPlaying
        state.togglePlay()
        DlnaRendererState.playbackState.value =
            if (wasPlaying) DlnaPlaybackState.PAUSED else DlnaPlaybackState.PLAYING
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
                        painter = painterResource(Res.drawable.arrow_left),
                        contentDescription = stringResource(Res.string.dlna_receiver_exit_player),
                        tint = Color.White,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(modifier = Modifier.weight(1f))
                DlnaDownloadIconButton()
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
                            painter = painterResource(Res.drawable.music2),
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.size(96.dp),
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(36.dp))
            Text(
                text = mediaTitle.ifEmpty { stringResource(Res.string.unknown) },
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.weight(1f))
            AudioPlayerControls(
                positionMs = state.currentTime,
                durationMs = state.totalTime,
                isPlaying = state.isPlaying,
                onPlayPause = onPlayPause,
                onSeek = { ratio ->
                    state.seekTo((ratio * state.totalTime.coerceAtLeast(1L)).toLong())
                },
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
