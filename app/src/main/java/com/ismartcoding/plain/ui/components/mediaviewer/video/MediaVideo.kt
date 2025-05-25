package com.ismartcoding.plain.ui.components.mediaviewer.video

import androidx.annotation.OptIn
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.PagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.media3.common.ForwardingPlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.session.MediaSession
import androidx.media3.ui.PlayerView
import com.ismartcoding.lib.extensions.pathToUri
import com.ismartcoding.plain.ui.components.mediaviewer.DEFAULT_OFFSET_X
import com.ismartcoding.plain.ui.components.mediaviewer.DEFAULT_OFFSET_Y
import com.ismartcoding.plain.ui.components.mediaviewer.DEFAULT_ROTATION
import com.ismartcoding.plain.ui.components.mediaviewer.DEFAULT_SCALE
import com.ismartcoding.plain.ui.components.mediaviewer.RawGesture
import com.ismartcoding.plain.ui.components.mediaviewer.SizeChangeContent
import com.ismartcoding.plain.ui.components.mediaviewer.detectTransformGestures
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
    scale: Float = DEFAULT_SCALE,
    offsetX: Float = DEFAULT_OFFSET_X,
    offsetY: Float = DEFAULT_OFFSET_Y,
    rotation: Float = DEFAULT_ROTATION,
    gesture: RawGesture = RawGesture(),
    onMounted: () -> Unit = {},
    onSizeChange: suspend (SizeChangeContent) -> Unit = {},
    boundClip: Boolean = true,
) {
    val scope = rememberCoroutineScope()
    val viewerAlpha = remember { Animatable(0F) }
    val context = LocalContext.current

    // 容器大小
    var bSize by remember { mutableStateOf(IntSize(0, 0)) }
    // 容器比例
    val bRatio by remember { derivedStateOf { bSize.width.toFloat() / bSize.height.toFloat() } }
    // 视频原始大小
    var vSize by remember { mutableStateOf(IntSize(0, 0)) }
    // 视频原始比例
    val vRatio by remember { derivedStateOf { if (vSize.height == 0) 1f else vSize.width.toFloat() / vSize.height.toFloat() } }
    // 是否宽度与容器大小一致
    var widthFixed by remember { mutableStateOf(false) }
    // 长宽是否均超出容器长宽
    val superSize by remember {
        derivedStateOf {
            vSize.height > bSize.height && vSize.width > bSize.width
        }
    }
    // 显示大小
    val uSize by remember {
        derivedStateOf {
            if (vSize == IntSize.Zero || bSize == IntSize.Zero) {
                bSize
            } else if (vRatio > bRatio) {
                // 宽度一致
                val uW = bSize.width
                val uH = uW / vRatio
                widthFixed = true
                IntSize(uW, uH.toInt())
            } else {
                // 高度一致
                val uH = bSize.height
                val uW = uH * vRatio
                widthFixed = false
                IntSize(uW.toInt(), uH)
            }
        }
    }
    // 视频显示的真实大小
    val rSize by remember {
        derivedStateOf {
            IntSize(
                (uSize.width * scale).toInt(),
                (uSize.height * scale).toInt()
            )
        }
    }

    LaunchedEffect(key1 = vSize, key2 = bSize, key3 = rSize) {
        if (vSize != IntSize.Zero && bSize != IntSize.Zero) {
            val maxScale = when {
                superSize -> {
                    vSize.width.toFloat() / uSize.width.toFloat()
                }
                widthFixed -> {
                    bSize.height.toFloat() / uSize.height.toFloat()
                }
                else -> {
                    bSize.width.toFloat() / uSize.width.toFloat()
                }
            }
            onSizeChange(
                SizeChangeContent(
                    defaultSize = uSize,
                    containerSize = bSize,
                    maxScale = maxScale
                )
            )
        }
    }

    // 视频是否加载成功
    var videoSpecified by remember { mutableStateOf(false) }

    // 初始化视频尺寸
    LaunchedEffect(model.path) {
        if (model.intrinsicSize == IntSize.Zero) {
            // 尝试从不同的数据源获取视频尺寸
            when (val data = model.data) {
                is com.ismartcoding.plain.data.DVideo -> {
                    model.initAsync(data)
                }
                else -> {
                    // 如果没有data，直接使用VideoHelper获取尺寸
                    val size = com.ismartcoding.plain.helpers.VideoHelper.getIntrinsicSize(model.path)
                    if (size != IntSize.Zero) {
                        model.intrinsicSize = size
                    }
                }
            }
        }
        if (model.intrinsicSize != IntSize.Zero) {
            vSize = model.intrinsicSize
            videoSpecified = true
        }
    }

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
                clip = boundClip
                alpha = viewerAlpha.value
            }
            .onSizeChanged {
                bSize = it
            }
            .pointerInput(Unit) {
                detectTapGestures(onLongPress = gesture.onLongPress)
            }
            .pointerInput(key1 = videoSpecified) {
                if (videoSpecified) detectTransformGestures(
                    onTap = gesture.onTap,
                    onDoubleTap = gesture.onDoubleTap,
                    gestureStart = gesture.gestureStart,
                    gestureEnd = gesture.gestureEnd,
                    onGesture = gesture.onGesture,
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        val videoModifier = Modifier
            .graphicsLayer {
                if (videoSpecified) {
                    scaleX = scale
                    scaleY = scale
                    translationX = offsetX
                    translationY = offsetY
                    rotationZ = rotation
                }
            }

        VideoPlayer(
            modifier = videoModifier
                .align(Alignment.Center)
                .size(
                    LocalDensity.current.run { uSize.width.toDp() },
                    LocalDensity.current.run { uSize.height.toDp() }
                ),
            player = player,
            playerView = defaultPlayerView,
            videoState = videoState,
        )
    }
}