package com.ismartcoding.plain.platform

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.pm.ActivityInfo
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.common.util.UnstableApi
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.Cache
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.LeastRecentlyUsedCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.ui.compose.PlayerSurface
import androidx.media3.ui.compose.SURFACE_TYPE_SURFACE_VIEW
import com.ismartcoding.plain.lib.extensions.isGestureInteractionMode
import com.ismartcoding.plain.lib.extensions.pathToUri
import com.ismartcoding.plain.mainActivity
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoPlayerController
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoPlayerEvent
import com.ismartcoding.plain.ui.components.mediaviewer.video.VideoState
import java.io.File
import java.util.UUID

@OptIn(UnstableApi::class)
@Composable
actual fun rememberVideoPlayerController(claimAudioSession: Boolean): VideoPlayerController {
    val context = LocalContext.current
    return remember {
        ExoPlayerVideoController(buildExoPlayer(context), context, claimAudioSession)
    }
}

@OptIn(UnstableApi::class)
private fun buildExoPlayer(context: Context): ExoPlayer {
    val httpDataSourceFactory = DefaultHttpDataSource.Factory()
    return ExoPlayer.Builder(context)
        .setSeekBackIncrementMs(10000L)
        .setSeekForwardIncrementMs(10000L)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setContentType(AUDIO_CONTENT_TYPE_MOVIE)
                .setUsage(C.USAGE_MEDIA)
                .build(),
            false,
        )
        .apply {
            val cache = VideoPlayerCacheManager.getCache()
            if (cache != null) {
                val cacheDataSourceFactory = CacheDataSource.Factory()
                    .setCache(cache)
                    .setUpstreamDataSourceFactory(DefaultDataSource.Factory(context, httpDataSourceFactory))
                setMediaSourceFactory(DefaultMediaSourceFactory(cacheDataSourceFactory))
            }
        }
        .build()
        .apply { videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT }
}

@SuppressLint("UnsafeOptInUsageError")
@Composable
actual fun VideoPlayerSurface(
    modifier: Modifier,
    controller: VideoPlayerController,
    videoState: VideoState?,
) {
    val exoController = controller as ExoPlayerVideoController
    val exoPlayer = exoController.exoPlayer
    val context = LocalContext.current
    val view = LocalView.current
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)
    var isPendingPipMode by remember { mutableStateOf(false) }

    // Track video size for aspect-ratio-aware "fit" rendering.
    // PlayerSurface (media3-ui-compose 1.10.x) has no resizeMode parameter,
    // so we apply Modifier.aspectRatio based on the reported VideoSize.
    var videoSize by remember { mutableStateOf<VideoSize?>(null) }
    var isBuffering by remember { mutableStateOf(false) }

    DisposableEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onVideoSizeChanged(size: VideoSize) {
                videoSize = size
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                isBuffering = playbackState == Player.STATE_BUFFERING
            }
        }
        exoPlayer.addListener(listener)
        onDispose { exoPlayer.removeListener(listener) }
    }

    // Fullscreen + lifecycle management — only when a VideoState is provided
    // (local media previewer). The DLNA receiver passes null and manages these
    // itself via setImmersiveFullscreen / DisposableEffect.
    if (videoState != null) {
        val isPlayingState by remember { derivedStateOf { videoState.isPlaying } }
        LaunchedEffect(isPlayingState) {
            view.keepScreenOn = isPlayingState
        }

        // Fullscreen: switch orientation + immersive system bars.
        val activity = context.findActivity()
        LaunchedEffect(videoState.isFullscreenMode) {
            if (videoState.isFullscreenMode) {
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                val insetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                if (context.isGestureInteractionMode()) {
                    insetsController.hide(WindowInsetsCompat.Type.systemBars())
                } else {
                    insetsController.hide(WindowInsetsCompat.Type.statusBars())
                }
                insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                WindowCompat.setDecorFitsSystemWindows(activity.window, false)
            } else {
                // Non-fullscreen: only reset orientation. System bar visibility
                // is owned by the MediaPreviewer (status bar follows showActions
                // for video, immersive for image) — do NOT touch bars here.
                // This LaunchedEffect also runs on the initial composition of
                // every video page (swipe); manipulating bars here would fight
                // the previewer's state and surface the gesture/3-button bar.
                activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
            }
        }
        DisposableEffect(Unit) {
            onDispose {
                if (videoState.isFullscreenMode) {
                    activity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
                    val insetsController = WindowCompat.getInsetsController(activity.window, activity.window.decorView)
                    insetsController.show(WindowInsetsCompat.Type.systemBars())
                    insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
                    WindowCompat.setDecorFitsSystemWindows(activity.window, true)
                }
            }
        }

        DisposableEffect(Unit) {
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_PAUSE -> {
                        if (!videoState.enablePip) {
                            exoPlayer.pause()
                        }
                        if (videoState.enablePip && exoPlayer.playWhenReady) {
                            isPendingPipMode = true
                            Handler(Looper.getMainLooper()).post {
                                if (enterPipMode(videoState)) {
                                    Handler(Looper.getMainLooper()).postDelayed({
                                        isPendingPipMode = false
                                    }, 500)
                                }
                            }
                        }
                    }

                    Lifecycle.Event.ON_RESUME -> {
                        videoState.enablePip = context.isActivityStatePipMode()
                        if (!videoState.enablePip) {
                            exoPlayer.play()
                        }
                    }

                    else -> {}
                }
            }
            val lifecycle = lifecycleOwner.value.lifecycle
            lifecycle.addObserver(observer)
            onDispose {
                lifecycle.removeObserver(observer)
            }
        }
    }

    // Shared PlayerSurface — rendering + buffering indicator always active.
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        PlayerSurface(
            player = exoPlayer,
            modifier = Modifier.then(fitModifier(videoSize)),
            surfaceType = SURFACE_TYPE_SURFACE_VIEW,
        )
        // Before the video size is reported, fitModifier falls back to
        // fillMaxSize which stretches the first frame to fill the entire
        // container. Hide the surface behind a black overlay until the
        // correct aspect ratio is applied.
        val vs = videoSize
        val sizeKnown = vs != null && vs.width > 0 && vs.height > 0
        if (!sizeKnown) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black))
        }
        if (isBuffering && sizeKnown) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = Color.White,
            )
        }
    }
}

/**
 * Builds a Modifier that preserves the video's aspect ratio (RESIZE_MODE_FIT behavior).
 * When [videoSize] is unknown or invalid, falls back to fillMaxSize to avoid distortion.
 */
@OptIn(UnstableApi::class)
private fun fitModifier(videoSize: VideoSize?): Modifier {
    if (videoSize == null || videoSize.width <= 0 || videoSize.height <= 0) {
        return Modifier.fillMaxSize()
    }
    val ratio = videoSize.width.toFloat() / videoSize.height.toFloat()
    return Modifier.aspectRatio(ratio)
}

// ---- Activity helpers ----

internal fun Context.findActivity(): Activity {
    return mainActivity!!
}

internal fun Context.isActivityStatePipMode(): Boolean {
    return findActivity().isInPictureInPictureMode
}

// ---- ExoPlayer-backed VideoPlayerController (moved from ui/components/mediaviewer/video/) ----

/**
 * Android implementation of [VideoPlayerController] backed by ExoPlayer.
 *
 * @param claimAudioSession when true, creates a [MediaSession] and wires audio
 *   focus management (for the local media previewer). When false, the controller
 *   is a raw ExoPlayer wrapper with no session/focus — used by the DLNA receiver.
 */
@OptIn(UnstableApi::class)
class ExoPlayerVideoController(
    val exoPlayer: ExoPlayer,
    context: Context,
    private val claimAudioSession: Boolean,
) : VideoPlayerController {

    private val focusManager = if (claimAudioSession) {
        VideoAudioFocusManager(
            context.getSystemService(Context.AUDIO_SERVICE) as AudioManager,
        )
    } else {
        null
    }

    private var eventListener: ((VideoPlayerEvent) -> Unit)? = null

    private val playerListener = object : Player.Listener {
        override fun onEvents(player: Player, events: Player.Events) {
            val listener = eventListener ?: return
            listener(
                VideoPlayerEvent.StateChanged(
                    isPlaying = player.isPlaying,
                    duration = player.duration.coerceAtLeast(0L),
                ),
            )
            if (events.contains(Player.EVENT_POSITION_DISCONTINUITY)) {
                listener(VideoPlayerEvent.PositionDiscontinuity)
            }
            if (events.contains(Player.EVENT_RENDERED_FIRST_FRAME)) {
                listener(VideoPlayerEvent.FirstFrameRendered)
            }
        }
    }

    private var mediaSession: MediaSession? = null

    init {
        exoPlayer.addListener(playerListener)
        if (claimAudioSession) {
            mediaSession = try {
                MediaSession.Builder(
                    context.applicationContext,
                    ForwardingPlayer(exoPlayer),
                ).setId("VideoPlayerMediaSession_" + UUID.randomUUID().toString().lowercase().split("-").first())
                    .build()
            } catch (e: Throwable) {
                null
            }
        }
    }

    override fun play() = exoPlayer.play()
    override fun pause() = exoPlayer.pause()
    override fun stop() = exoPlayer.stop()
    override fun prepare() = exoPlayer.prepare()
    override fun seekTo(positionMs: Long) = exoPlayer.seekTo(positionMs)
    override fun setPlaybackSpeed(speed: Float) = exoPlayer.setPlaybackSpeed(speed)
    override fun setMuted(muted: Boolean) {
        exoPlayer.volume = if (muted) 0f else 1f
    }

    override fun release() {
        mediaSession?.release()
        mediaSession = null
        focusManager?.abandonFocus()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }

    override fun setMediaItem(path: String) {
        exoPlayer.setMediaItem(MediaItem.fromUri(path.pathToUri()))
    }

    override fun setEventListener(listener: (VideoPlayerEvent) -> Unit) {
        eventListener = listener
    }

    override fun requestAudioFocus() {
        focusManager?.requestFocus(exoPlayer)
    }
    override fun abandonAudioFocus() {
        focusManager?.abandonFocus()
    }

    override val duration: Long get() = exoPlayer.duration.coerceAtLeast(0L)
    override val currentPosition: Long get() = exoPlayer.currentPosition.coerceAtLeast(0L)
    override val bufferedPercentage: Int get() = exoPlayer.bufferedPercentage
    override val isPlaying: Boolean get() = exoPlayer.isPlaying
    override val isBuffering: Boolean get() = exoPlayer.playbackState == ExoPlayer.STATE_BUFFERING
}

// ---- VideoAudioFocusManager (moved from ui/components/mediaviewer/video/) ----

/**
 * Manages audio focus for the video previewer using AUDIOFOCUS_GAIN_TRANSIENT.
 * This ensures the background music player receives AUDIOFOCUS_LOSS_TRANSIENT
 * (not the permanent AUDIOFOCUS_LOSS), so it auto-resumes when we release focus.
 */
class VideoAudioFocusManager(private val audioManager: AudioManager) {
    private var focusRequest: AudioFocusRequest? = null

    fun requestFocus(player: ExoPlayer) {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        val req = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
            .setAudioAttributes(
                android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_MOVIE)
                    .setUsage(android.media.AudioAttributes.USAGE_MEDIA)
                    .build()
            )
            .setOnAudioFocusChangeListener { change ->
                when (change) {
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT,
                    AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> player.pause()
                    AudioManager.AUDIOFOCUS_GAIN -> player.play()
                    AudioManager.AUDIOFOCUS_LOSS -> player.stop()
                }
            }
            .build()
        focusRequest = req
        audioManager.requestAudioFocus(req)
    }

    fun abandonFocus() {
        focusRequest?.let { audioManager.abandonAudioFocusRequest(it) }
        focusRequest = null
    }
}

// ---- VideoPlayerCacheManager (moved from ui/components/mediaviewer/video/) ----

@OptIn(UnstableApi::class)
object VideoPlayerCacheManager {

    private lateinit var cacheInstance: Cache

    /**
     * Set the cache for video player.
     * It can only be set once in the app, and it is shared and used by multiple video players.
     *
     * @param context Current activity context.
     * @param maxCacheBytes Sets the maximum cache capacity in bytes. If the cache builds up as much as the set capacity, it is deleted from the oldest cache.
     */
    @SuppressLint("UnsafeOptInUsageError")
    fun initialize(context: Context, maxCacheBytes: Long) {
        if (VideoPlayerCacheManager::cacheInstance.isInitialized) {
            return
        }

        cacheInstance = SimpleCache(
            File(context.cacheDir, "video"),
            LeastRecentlyUsedCacheEvictor(maxCacheBytes),
            StandaloneDatabaseProvider(context),
        )
    }

    /**
     * Gets the ExoPlayer cache instance. If null, the cache to be disabled.
     */
    internal fun getCache(): Cache? =
        if (VideoPlayerCacheManager::cacheInstance.isInitialized) {
            cacheInstance
        } else {
            null
        }
}
