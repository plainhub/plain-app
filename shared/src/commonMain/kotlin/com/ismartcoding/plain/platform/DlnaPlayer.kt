package com.ismartcoding.plain.platform

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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.runtime.mutableIntStateOf
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
import com.ismartcoding.plain.lib.extensions.formatMinSec
import com.ismartcoding.plain.ui.base.PlayerSlider
import com.ismartcoding.plain.ui.page.dlna.AudioPlayerControls
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.time.Duration.Companion.seconds

/**
 * Raw media-playback engine for the DLNA receiver player UI.
 *
 * Deliberately distinct from [com.ismartcoding.plain.ui.components.mediaviewer.video.VideoPlayerController]:
 * the DLNA receiver plays media pushed by a remote sender and must NOT claim a
 * MediaSession or audio focus (which would contend with the app's own audio
 * player and surface lock-screen controls). Platform actuals wrap a raw
 * ExoPlayer (Android) / AVPlayer (iOS) with no session or focus management.
 */
interface DlnaMediaEngine {
    fun setMediaUri(uri: String)
    fun play()
    fun pause()

    /** Pause and rewind to the start (matches DLNA STOPPED semantics). */
    fun stopAndRewind()
    fun seekTo(positionMs: Long)
    val positionMs: Long
    val durationMs: Long
    val isPlaying: Boolean
    fun release()
}

/**
 * Creates and remembers a platform-specific [DlnaMediaEngine].
 * Android: raw ExoPlayer; iOS: AVPlayer.
 */
@Composable
expect fun rememberDlnaMediaEngine(): DlnaMediaEngine

/**
 * Platform video rendering surface attached to [engine], with no built-in
 * controls (the DLNA overlay draws its own). Android: PlayerView; iOS: AVPlayerLayer.
 */
@Composable
expect fun DlnaVideoSurface(engine: DlnaMediaEngine, modifier: Modifier)

@Composable
fun DlnaReceiverAudioPlayerContent(onExit: () -> Unit) {
    val mediaUri by DlnaRendererState.mediaUri.collectAsState()
    val mediaTitle by DlnaRendererState.mediaTitle.collectAsState()
    val albumArtUri by DlnaRendererState.mediaAlbumArtUri.collectAsState()
    var albumArtFailed by remember(albumArtUri) { mutableStateOf(false) }
    val playbackState by DlnaRendererState.playbackState.collectAsState()
    val seekTargetMs by DlnaRendererState.seekTargetMs.collectAsState()
    val positionMs by DlnaRendererState.currentPositionMs.collectAsState()
    val durationMs by DlnaRendererState.durationMs.collectAsState()

    val engine = rememberDlnaMediaEngine()
    val context = LocalPlatformContext.current

    LaunchedEffect(mediaUri) {
        if (mediaUri.isNotEmpty()) {
            engine.setMediaUri(mediaUri)
        }
    }
    LaunchedEffect(playbackState) {
        when (playbackState) {
            DlnaPlaybackState.PLAYING -> engine.play()
            DlnaPlaybackState.PAUSED -> engine.pause()
            DlnaPlaybackState.STOPPED -> engine.stopAndRewind()
            else -> {}
        }
    }
    LaunchedEffect(seekTargetMs) { seekTargetMs?.let { engine.seekTo(it) } }
    LaunchedEffect(engine) {
        while (true) {
            DlnaRendererState.currentPositionMs.value = engine.positionMs
            DlnaRendererState.durationMs.value = engine.durationMs
            delay(1_000)
        }
    }
    setImmersiveFullscreen()
    DisposableEffect(Unit) {
        onDispose { exitImmersiveFullscreen(); engine.release() }
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
                positionMs = positionMs,
                durationMs = durationMs,
                isPlaying = playbackState == DlnaPlaybackState.PLAYING,
                onPlayPause = {
                    if (engine.isPlaying) {
                        engine.pause()
                        DlnaRendererState.playbackState.value = DlnaPlaybackState.PAUSED
                    } else {
                        engine.play()
                        DlnaRendererState.playbackState.value = DlnaPlaybackState.PLAYING
                    }
                },
                onSeek = { ratio -> engine.seekTo((ratio * durationMs).toLong()) },
            )
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun DlnaReceiverVideoPlayerContent(onExit: () -> Unit) {
    val mediaUri by DlnaRendererState.mediaUri.collectAsState()
    val mediaTitle by DlnaRendererState.mediaTitle.collectAsState()
    val playbackState by DlnaRendererState.playbackState.collectAsState()
    val seekTargetMs by DlnaRendererState.seekTargetMs.collectAsState()
    val positionMs by DlnaRendererState.currentPositionMs.collectAsState()
    val durationMs by DlnaRendererState.durationMs.collectAsState()

    val engine = rememberDlnaMediaEngine()
    var showControls by remember { mutableStateOf(true) }
    var controlsSeed by remember { mutableIntStateOf(0) }

    LaunchedEffect(mediaUri) {
        if (mediaUri.isNotEmpty()) {
            engine.setMediaUri(mediaUri)
        }
    }
    LaunchedEffect(playbackState) {
        when (playbackState) {
            DlnaPlaybackState.PLAYING -> engine.play()
            DlnaPlaybackState.PAUSED -> engine.pause()
            DlnaPlaybackState.STOPPED -> engine.stopAndRewind()
            else -> {}
        }
    }
    LaunchedEffect(seekTargetMs) { seekTargetMs?.let { engine.seekTo(it) } }
    LaunchedEffect(engine) {
        while (true) {
            DlnaRendererState.currentPositionMs.value = engine.positionMs
            DlnaRendererState.durationMs.value = engine.durationMs
            delay(1_000)
        }
    }
    LaunchedEffect(showControls, controlsSeed) {
        if (showControls) { delay(4.seconds); showControls = false }
    }
    setImmersiveFullscreen()
    DisposableEffect(Unit) {
        onDispose { exitImmersiveFullscreen(); engine.release() }
    }

    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black)
            .clickable(indication = null, interactionSource = interactionSource) {
                showControls = !showControls; if (showControls) controlsSeed++
            },
    ) {
        DlnaVideoSurface(engine = engine, modifier = Modifier.fillMaxSize())
        AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
            DlnaVideoControls(
                mediaTitle = mediaTitle, positionMs = positionMs, durationMs = durationMs,
                isPlaying = playbackState == DlnaPlaybackState.PLAYING,
                onPlayPause = {
                    if (engine.isPlaying) {
                        engine.pause()
                        DlnaRendererState.playbackState.value = DlnaPlaybackState.PAUSED
                    } else {
                        engine.play()
                        DlnaRendererState.playbackState.value = DlnaPlaybackState.PLAYING
                    }
                },
                onSeek = { ratio -> engine.seekTo((ratio * durationMs).toLong()) },
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
                Icon(painter = painterResource(Res.drawable.arrow_left), contentDescription = stringResource(Res.string.dlna_receiver_exit_player), tint = Color.White, modifier = Modifier.size(24.dp))
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
                        Icon(painter = painterResource(if (isPlaying) Res.drawable.pause else Res.drawable.play_arrow), contentDescription = null, tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                }
                Text(text = durationMs.formatMinSec(), style = MaterialTheme.typography.labelSmall, color = Color.White, modifier = Modifier.width(52.dp))
            }
        }
    }
}

@Composable
fun DlnaReceiverImageViewerContent(onExit: () -> Unit) {
    val mediaUri by DlnaRendererState.mediaUri.collectAsState()
    val mediaTitle by DlnaRendererState.mediaTitle.collectAsState()
    val context = LocalPlatformContext.current

    var showControls by remember { mutableStateOf(true) }
    var isLoading by remember(mediaUri) { mutableStateOf(true) }
    val interactionSource = remember { MutableInteractionSource() }

    setImmersiveFullscreen()
    DisposableEffect(Unit) {
        onDispose { exitImmersiveFullscreen() }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(indication = null, interactionSource = interactionSource) {
                showControls = !showControls
            },
        contentAlignment = Alignment.Center,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context).data(mediaUri).build(),
            contentDescription = mediaTitle,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
            onLoading = { isLoading = true },
            onSuccess = { isLoading = false },
            onError = { isLoading = false },
        )

        if (isLoading) {
            CircularProgressIndicator(
                color = Color.White,
                modifier = Modifier.size(48.dp),
            )
        }

        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.TopStart),
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black.copy(alpha = 0.5f))
                    .statusBarsPadding()
                    .padding(4.dp),
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
                Text(
                    text = mediaTitle,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp),
                )
            }
        }
    }
}
