package com.ismartcoding.plain.platform

import android.annotation.SuppressLint
import android.app.Activity
import android.app.PictureInPictureParams
import android.content.Context
import android.content.pm.ActivityInfo
import android.content.pm.PackageManager
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Handler
import android.os.Looper
import android.util.Rational
import android.widget.ImageButton
import androidx.annotation.OptIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
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
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
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
actual fun rememberVideoPlayerController(): VideoPlayerController {
    val context = LocalContext.current
    return remember {
        ExoPlayerVideoController(buildExoPlayer(context), context)
    }
}

/**
 * Android-only raw ExoPlayer factory for callers (e.g. DLNA receivers) that
 * use the ExoPlayer API directly and don't need the [VideoPlayerController]
 * abstraction (MediaSession / audio focus / event mapping).
 */
@OptIn(UnstableApi::class)
@Composable
internal fun rememberExoPlayer(context: Context): ExoPlayer {
    return remember { buildExoPlayer(context) }
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
    videoState: VideoState,
    useController: Boolean,
) {
    val exoController = controller as ExoPlayerVideoController
    val exoPlayer = exoController.exoPlayer
    val context = LocalContext.current
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)

    val playerView = remember { PlayerView(context) }
    var isPendingPipMode by remember { mutableStateOf(false) }

    val isPlayingState by remember { derivedStateOf { videoState.isPlaying } }
    LaunchedEffect(isPlayingState) {
        playerView.keepScreenOn = isPlayingState
    }

    AndroidView(
        modifier = modifier,
        factory = {
            playerView.apply {
                this.player = exoPlayer
                this.useController = useController
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
    )

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
                    if (videoState.enablePip && exoPlayer.playWhenReady) {
                        playerView.useController = useController
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

    if (videoState.isFullscreenMode) {
        VideoPlayerFullScreenDialog(
            exoPlayer = exoPlayer,
            currentPlayerView = playerView,
            videoState = videoState,
        )
    }
}

// ---- PiP (androidMain-only, called from surface actual and VideoPreviewButtons) ----

internal fun hasPipMode(context: Context): Boolean {
    return context.packageManager.hasSystemFeature(PackageManager.FEATURE_PICTURE_IN_PICTURE)
}

internal fun enterPipMode(videoState: VideoState): Boolean {
    val context = mainActivity ?: return false
    if (!hasPipMode(context)) return false
    videoState.enablePip = true
    val params = PictureInPictureParams.Builder()
    if (isTPlus()) {
        params
            .setTitle("Video Player")
            .setAspectRatio(Rational(16, 9))
            .setSeamlessResizeEnabled(true)
    }
    return runCatching {
        context.findActivity().enterPictureInPictureMode(params.build())
        true
    }.getOrDefault(false)
}

// ---- Fullscreen dialog (androidMain-only) ----

@SuppressLint("UnsafeOptInUsageError")
@Composable
internal fun VideoPlayerFullScreenDialog(
    exoPlayer: ExoPlayer,
    currentPlayerView: PlayerView,
    videoState: VideoState,
) {
    val context = LocalContext.current
    val fullScreenPlayerView = remember {
        PlayerView(context).apply {
            setShowBuffering(PlayerView.SHOW_BUFFERING_ALWAYS)
        }
    }

    val view = LocalView.current
    val window = (view.context as Activity).window
    val insetsController = WindowCompat.getInsetsController(window, view)

    val onDismissRequest = {
        PlayerView.switchTargetView(exoPlayer, fullScreenPlayerView, currentPlayerView)
        currentPlayerView.findViewById<ImageButton>(androidx.media3.ui.R.id.exo_fullscreen)
            .performClick()
        val currentActivity = context.findActivity()
        currentActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        currentActivity.setFullScreen(false)
        if (context.isGestureInteractionMode()) {
            insetsController.show(WindowInsetsCompat.Type.systemBars())
        } else {
            insetsController.show(WindowInsetsCompat.Type.statusBars())
        }
        videoState.isFullscreenMode = false
    }

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnClickOutside = false,
            usePlatformDefaultWidth = false,
            securePolicy = SecureFlagPolicy.Inherit,
            decorFitsSystemWindows = false,
        ),
    ) {
        LaunchedEffect(Unit) {
            PlayerView.switchTargetView(exoPlayer, currentPlayerView, fullScreenPlayerView)

            val currentActivity = context.findActivity()
            currentActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            (view.parent as DialogWindowProvider).window.setFullScreen(true)
            fullScreenPlayerView.setFullscreenButtonClickListener {
                if (!it) {
                    onDismissRequest()
                }
            }
            fullScreenPlayerView.findViewById<ImageButton>(androidx.media3.ui.R.id.exo_fullscreen)
                .performClick()
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
        ) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = {
                    fullScreenPlayerView.apply {
                        this.player = exoPlayer
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    }
                },
            )
        }
    }
}

// ---- Activity/Window helpers (moved from Helpers.kt) ----

internal fun Context.findActivity(): Activity {
    return mainActivity!!
}

internal fun Context.isActivityStatePipMode(): Boolean {
    return findActivity().isInPictureInPictureMode
}

internal fun Activity.setFullScreen(fullscreen: Boolean) {
    window.setFullScreen(fullscreen)
}

@Suppress("Deprecation")
internal fun android.view.Window.setFullScreen(fullscreen: Boolean) {
    if (fullscreen) {
        decorView.systemUiVisibility = (
            android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                or android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                or android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
            )
    } else {
        decorView.systemUiVisibility = android.view.View.SYSTEM_UI_FLAG_VISIBLE
    }
}

// ---- ExoPlayer-backed VideoPlayerController (moved from ui/components/mediaviewer/video/) ----

/**
 * Android implementation of [VideoPlayerController] backed by ExoPlayer.
 */
@OptIn(UnstableApi::class)
class ExoPlayerVideoController(
    val exoPlayer: ExoPlayer,
    context: Context,
) : VideoPlayerController {

    private val focusManager = VideoAudioFocusManager(
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager,
    )

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
        focusManager.abandonFocus()
        exoPlayer.removeListener(playerListener)
        exoPlayer.release()
    }

    override fun setMediaItem(path: String) {
        exoPlayer.setMediaItem(MediaItem.fromUri(path.pathToUri()))
    }

    override fun setEventListener(listener: (VideoPlayerEvent) -> Unit) {
        eventListener = listener
    }

    override fun requestAudioFocus() = focusManager.requestFocus(exoPlayer)
    override fun abandonAudioFocus() = focusManager.abandonFocus()

    override val duration: Long get() = exoPlayer.duration.coerceAtLeast(0L)
    override val currentPosition: Long get() = exoPlayer.currentPosition.coerceAtLeast(0L)
    override val bufferedPercentage: Int get() = exoPlayer.bufferedPercentage
    override val isPlaying: Boolean get() = exoPlayer.isPlaying
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
