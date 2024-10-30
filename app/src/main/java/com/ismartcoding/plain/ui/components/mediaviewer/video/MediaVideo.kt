package com.ismartcoding.plain.ui.components.mediaviewer.video

import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
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
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import com.ismartcoding.lib.extensions.pathToUri
import com.ismartcoding.plain.ui.components.mediaviewer.RawGesture
import com.ismartcoding.plain.ui.components.mediaviewer.previewer.DEFAULT_CROSS_FADE_ANIMATE_SPEC
import com.ismartcoding.plain.ui.components.mediaviewer.PreviewItem
import kotlinx.coroutines.launch
import java.util.UUID


@kotlin.OptIn(ExperimentalFoundationApi::class)
@OptIn(UnstableApi::class)
@Composable
fun MediaVideo(
    modifier: Modifier = Modifier,
    pagerState: PagerState,
    videoState: VideoState,
    page: Int,
    model: PreviewItem,
    gesture: RawGesture = RawGesture(),
    onMounted: () -> Unit = {},
) {
    val scope = rememberCoroutineScope()
    val viewerAlpha = remember { Animatable(0F) }
    val context = LocalContext.current

    val defaultPlayerView = remember {
        PlayerView(context)
    }

    var mediaSession = remember<MediaSession?> { null }
    val player = rememberVideoPlayer(context, playerInstance = {
        addListener(
            object : Player.Listener {
                override fun onEvents(player: Player, events: Player.Events) {
                    scope.launch {
                        videoState.totalTime = player.duration.coerceAtLeast(0L)
                        videoState.isPlaying = player.isPlaying
                        if (!videoState.isSeeking) {
                            videoState.updateTime()
                        }
                        defaultPlayerView.keepScreenOn = player.isPlaying
                    }
                }
            }
        )
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
        videoState.initData(player)
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
                clip = true
                alpha = viewerAlpha.value
            }
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = gesture.onLongPress, onTap = gesture.onTap)
            },
        contentAlignment = Alignment.Center,
    ) {
        VideoPlayer(
            modifier = Modifier
                .align(Alignment.Center),
            player = player,
            playerView = defaultPlayerView,
            videoState = videoState,
        )
    }
}