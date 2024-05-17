package com.ismartcoding.plain.ui.components.mediaviewer

import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.analytics.AnalyticsListener
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import com.ismartcoding.lib.extensions.pathToUri
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.DEFAULT_CROSS_FADE_ANIMATE_SPEC
import com.ismartcoding.plain.ui.base.videoplayer.VideoPlayer
import com.ismartcoding.plain.ui.base.videoplayer.VideoPlayerMediaItem
import com.ismartcoding.plain.ui.base.videoplayer.rememberVideoPlayer
import com.ismartcoding.plain.ui.base.videoplayer.toUri
import com.ismartcoding.plain.ui.preview.PreviewItem
import kotlinx.coroutines.launch
import java.util.UUID

@kotlin.OptIn(ExperimentalFoundationApi::class)
@OptIn(UnstableApi::class)
@Composable
fun MediaVideo(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    page: Int,
    model: PreviewItem,
    gesture: RawGesture = RawGesture(),
    onMounted: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val viewerAlpha = remember { Animatable(0F) }
    val view = LocalView.current
    val context = LocalContext.current

    var mediaSession = remember<MediaSession?> { null }
    val defaultPlayerView = remember {
        PlayerView(context)
    }
    val player = rememberVideoPlayer(context, playerInstance = {
        addAnalyticsListener(object : AnalyticsListener {
            override fun onPlayWhenReadyChanged(
                eventTime: AnalyticsListener.EventTime,
                playWhenReady: Boolean,
                reason: Int,
            ) {
            }

            override fun onIsPlayingChanged(eventTime: AnalyticsListener.EventTime, isPlaying: Boolean) {
                defaultPlayerView.keepScreenOn = isPlaying
            }

            override fun onVolumeChanged(
                eventTime: AnalyticsListener.EventTime,
                volume: Float,
            ) {
            }
        })
    })

    fun goMounted() {
        scope.launch {
            viewerAlpha.animateTo(1F, DEFAULT_CROSS_FADE_ANIMATE_SPEC)
            onMounted()
        }
    }

    goMounted()

    LaunchedEffect(player, pagerState.currentPage) {
        if (pagerState.currentPage != page) {
            return@LaunchedEffect
        }
        mediaSession?.release()
        mediaSession = MediaSession.Builder(context, ForwardingPlayer(player))
            .setId("VideoPlayerMediaSession_${UUID.randomUUID().toString().lowercase().split("-").first()}")
            .build()
        val exoPlayerMediaItems = listOf(
            VideoPlayerMediaItem.StorageMediaItem(
                storageUri = model.path.pathToUri(),
            )
        ).map {
            val uri = it.toUri(context)
            MediaItem.Builder().apply {
                setUri(uri)
                setMediaMetadata(it.mediaMetadata)
                setMimeType(it.mimeType)
                setDrmConfiguration(null)
            }.build()
        }

        player.setMediaItems(exoPlayerMediaItems)
        player.prepare()
        player.play()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .graphicsLayer {
                // 图片位移时会超出容器大小，需要在这个地方指定是否裁切
                clip = true
                alpha = viewerAlpha.value
            }
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = gesture.onLongPress)
            },
        contentAlignment = Alignment.Center,
    ) {
        VideoPlayer(
            modifier = Modifier
                .align(Alignment.Center),
            player = player,
            playerView = defaultPlayerView,
        )
    }
}