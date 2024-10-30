package com.ismartcoding.plain.ui.components.mediaviewer.video

import android.annotation.SuppressLint
import android.os.Handler
import android.os.Looper
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView


@SuppressLint("UnsafeOptInUsageError")
@Composable
internal fun VideoPlayerSurface(
    modifier: Modifier = Modifier,
    playerView: PlayerView,
    player: ExoPlayer,
    videoState: VideoState,
    usePlayerController: Boolean,
    onPipEntered: () -> Unit = {},
    autoDispose: Boolean = true,
) {
    val lifecycleOwner = rememberUpdatedState(LocalLifecycleOwner.current)
    val context = LocalContext.current

    var isPendingPipMode by remember { mutableStateOf(false) }

    AndroidView(
        modifier = modifier,
        factory = {
            playerView.apply {
                this.player = player
                useController = usePlayerController
                resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            }
        },
    )
    DisposableEffect(
        Unit,
    ) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> {
                    if (!videoState.enablePip) {
                        player.pause()
                    }
                    if (videoState.enablePip && player.playWhenReady) {
                        isPendingPipMode = true

                        Handler(Looper.getMainLooper()).post {
                            videoState.enterPipMode(context)
                            onPipEntered()

                            Handler(Looper.getMainLooper()).postDelayed({
                                isPendingPipMode = false
                            }, 500)
                        }
                    }
                }

                Lifecycle.Event.ON_RESUME -> {
                    videoState.enablePip = context.isActivityStatePipMode()
                    if (!videoState.enablePip) {
                        player.play()
                    }
                    if (videoState.enablePip && player.playWhenReady) {
                        playerView.useController = usePlayerController
                    }
                }

                Lifecycle.Event.ON_STOP -> {
                   // player.stop()
                }

                else -> {}
            }
        }
        val lifecycle = lifecycleOwner.value.lifecycle
        lifecycle.addObserver(observer)

        onDispose {
            if (autoDispose) {
                player.release()
                lifecycle.removeObserver(observer)
            }
        }
    }
}
