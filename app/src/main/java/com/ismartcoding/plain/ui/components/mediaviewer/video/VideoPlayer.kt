package com.ismartcoding.plain.ui.components.mediaviewer.video

import android.annotation.SuppressLint
import android.content.Context
import androidx.annotation.OptIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.C.AUDIO_CONTENT_TYPE_MOVIE
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.ismartcoding.lib.logcat.LogCat
import kotlinx.coroutines.delay
import kotlin.time.Duration.Companion.seconds

@SuppressLint("SourceLockedOrientationActivity", "UnsafeOptInUsageError")
@Composable
fun VideoPlayer(
    modifier: Modifier = Modifier,
    player: ExoPlayer,
    playerView: PlayerView,
    videoState: VideoState,
) {
    VideoPlayerSurface(
        modifier = modifier,
        playerView = playerView,
        player = player,
        videoState = videoState,
        usePlayerController = false,
    )

    // because the video player is in pager, we need to manually pause the player when it's not visible
    if (videoState.isFullscreenMode && videoState.player == player) {
        VideoPlayerFullScreenDialog(
            player = player,
            currentPlayerView = playerView,
            videoState = videoState,
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
            .apply {
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT
            }
            .also(playerInstance)
    }
}
