package com.ismartcoding.plain.ui.page.dlna

import androidx.annotation.OptIn
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.ismartcoding.lib.extensions.formatMinSec
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.dlna.DlnaPlaybackState
import com.ismartcoding.plain.features.dlna.DlnaRendererState
import com.ismartcoding.plain.ui.base.PlayerSlider
import com.ismartcoding.plain.ui.components.mediaviewer.video.rememberVideoPlayer
import com.ismartcoding.plain.ui.components.mediaviewer.video.setFullScreen
import com.ismartcoding.plain.ui.components.mediaviewer.video.findActivity
import com.ismartcoding.plain.ui.models.DlnaReceiverViewModel
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@OptIn(UnstableApi::class)
@Composable
fun DlnaReceiverVideoPlayer(vm: DlnaReceiverViewModel, onExit: () -> Unit) {
    val context = LocalContext.current
    val mediaUri by DlnaRendererState.mediaUri.collectAsState()
    val mediaTitle by DlnaRendererState.mediaTitle.collectAsState()
    val playbackState by DlnaRendererState.playbackState.collectAsState()
    val seekTargetMs by DlnaRendererState.seekTargetMs.collectAsState()
    val positionMs by DlnaRendererState.currentPositionMs.collectAsState()
    val durationMs by DlnaRendererState.durationMs.collectAsState()

    val player = rememberVideoPlayer(context)
    val playerView = remember { PlayerView(context) }
    var showControls by remember { mutableStateOf(true) }
    var controlsSeed by remember { mutableIntStateOf(0) }

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
    LaunchedEffect(showControls, controlsSeed) {
        if (showControls) { delay(4.seconds); showControls = false }
    }
    DisposableEffect(Unit) {
        context.findActivity().setFullScreen(true)
        onDispose { context.findActivity().setFullScreen(false); player.stop(); player.release() }
    }

    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black)
            .clickable(indication = null, interactionSource = interactionSource) {
                showControls = !showControls; if (showControls) controlsSeed++
            },
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { playerView.apply { this.player = player; useController = false; resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT } },
        )
        AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
            DlnaVideoControls(
                mediaTitle = mediaTitle, positionMs = positionMs, durationMs = durationMs,
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
                onExit = onExit,
            )
        }
    }
}

@Composable
private fun DlnaVideoControls(
    mediaTitle: String,
    positionMs: Long,
    durationMs: Long,
    isPlaying: Boolean,
    onPlayPause: () -> Unit,
    onSeek: (Float) -> Unit,
    onExit: () -> Unit,
) {
    val darkOverlay = Color.Black.copy(alpha = 0.55f)
    Column(modifier = Modifier.fillMaxSize()) {
        // Top bar
        Row(
            modifier = Modifier.fillMaxWidth().background(darkOverlay).statusBarsPadding().padding(4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onExit) {
                Icon(painter = painterResource(R.drawable.arrow_left), contentDescription = stringResource(R.string.dlna_receiver_exit_player), tint = Color.White, modifier = Modifier.size(24.dp))
            }
            Text(text = mediaTitle, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Medium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f).padding(end = 8.dp))
        }
        // Center spacer
        Box(modifier = Modifier.weight(1f))
        // Bottom controls
        Column(
            modifier = Modifier.fillMaxWidth().background(darkOverlay).navigationBarsPadding().padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            val progress = if (durationMs > 0) positionMs.toFloat() / durationMs else 0f
            PlayerSlider(modifier = Modifier.fillMaxWidth().height(24.dp), progress = progress, bufferedProgress = 0f, onProgressChange = onSeek)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = positionMs.formatMinSec(), style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.width(52.dp))
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    IconButton(modifier = Modifier.size(48.dp), onClick = onPlayPause) {
                        Icon(painter = painterResource(if (isPlaying) R.drawable.pause else R.drawable.play_arrow), contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
                Text(text = durationMs.formatMinSec(), style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.width(52.dp))
            }
        }
    }
}
