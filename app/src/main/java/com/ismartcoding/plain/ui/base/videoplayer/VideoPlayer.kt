package com.ismartcoding.plain.ui.base.videoplayer

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.ActivityInfo
import android.widget.ImageButton
import androidx.activity.compose.BackHandler
import androidx.annotation.FloatRange
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE
import androidx.media3.common.Player
import androidx.media3.common.util.RepeatModeUtil.REPEAT_TOGGLE_MODE_ALL
import androidx.media3.common.util.RepeatModeUtil.REPEAT_TOGGLE_MODE_NONE
import androidx.media3.common.util.RepeatModeUtil.REPEAT_TOGGLE_MODE_ONE
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView

@SuppressLint("SourceLockedOrientationActivity", "UnsafeOptInUsageError")
@Composable
fun VideoPlayer(
    modifier: Modifier = Modifier,
    player: ExoPlayer,
    playerView: PlayerView,
    usePlayerController: Boolean = true,
    controllerConfig: VideoPlayerControllerConfig = VideoPlayerControllerConfig(),
    repeatMode: RepeatMode = RepeatMode.NONE,
    resizeMode: ResizeMode = ResizeMode.FIT,
    @FloatRange(from = 0.0, to = 1.0) volume: Float = 1f,
    fullScreenSecurePolicy: SecureFlagPolicy = SecureFlagPolicy.Inherit,
    onFullScreenEnter: () -> Unit = {},
    onFullScreenExit: () -> Unit = {},
    enablePip: Boolean = false,
    defaultFullScreen: Boolean = false,
    enablePipWhenBackPressed: Boolean = false,
) {
    val context = LocalContext.current
    BackHandler(enablePip && enablePipWhenBackPressed) {
        enterPIPMode(context, playerView)
        player.play()
    }

    LaunchedEffect(usePlayerController) {
        playerView.useController = usePlayerController
    }

    LaunchedEffect(player) {
        playerView.player = player
    }

    var isFullScreenModeEntered by remember { mutableStateOf(defaultFullScreen) }

    LaunchedEffect(controllerConfig) {
        controllerConfig.applyToExoPlayerView(playerView) {
            isFullScreenModeEntered = it

            if (it) {
                onFullScreenEnter()
            }
        }
    }

    LaunchedEffect(controllerConfig, repeatMode) {
        playerView.setRepeatToggleModes(
            if (controllerConfig.showRepeatModeButton) {
                REPEAT_TOGGLE_MODE_ALL or REPEAT_TOGGLE_MODE_ONE
            } else {
                REPEAT_TOGGLE_MODE_NONE
            },
        )
        player.repeatMode = repeatMode.toExoPlayerRepeatMode()
    }

    LaunchedEffect(volume) {
        player.volume = volume
    }

    VideoPlayerSurface(
        modifier = modifier,
        defaultPlayerView = playerView,
        player = player,
        usePlayerController = usePlayerController,
        enablePip = enablePip,
        surfaceResizeMode = resizeMode
    )

    if (isFullScreenModeEntered) {
        var fullScreenPlayerView by remember { mutableStateOf<PlayerView?>(null) }

        VideoPlayerFullScreenDialog(
            player = player,
            currentPlayerView = playerView,
            controllerConfig = controllerConfig,
            repeatMode = repeatMode,
            resizeMode = resizeMode,
            onDismissRequest = {
                fullScreenPlayerView?.let {
                    PlayerView.switchTargetView(player, it, playerView)
                    playerView.findViewById<ImageButton>(androidx.media3.ui.R.id.exo_fullscreen)
                        .performClick()
                    val currentActivity = context.findActivity()
                    currentActivity.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                    currentActivity.setFullScreen(false)
                    onFullScreenExit()
                }

                isFullScreenModeEntered = false
            },
            securePolicy = fullScreenSecurePolicy,
            enablePip = enablePip,
            fullScreenPlayerView = {
                fullScreenPlayerView = this
            },
        )
    }
}

@OptIn(UnstableApi::class)
@Composable
fun rememberVideoPlayer(context: Context, playerInstance: ExoPlayer.() -> Unit = {}): ExoPlayer {
    return remember {
        val httpDataSourceFactory = DefaultHttpDataSource.Factory()

        ExoPlayer.Builder(context)
            .setSeekBackIncrementMs(10000L)
            .setSeekForwardIncrementMs(10000L)
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AUDIO_CONTENT_TYPE_MOVIE)
                    .setUsage(C.USAGE_MEDIA)
                    .build(),
                true,
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
            .also(playerInstance)
    }
}
