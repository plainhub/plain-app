package com.ismartcoding.plain.ui.components.mediaviewer.video

import android.annotation.SuppressLint
import android.app.Activity
import android.content.pm.ActivityInfo
import android.widget.ImageButton
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import androidx.media3.ui.R
import com.ismartcoding.lib.extensions.isGestureInteractionMode
import com.ismartcoding.lib.logcat.LogCat

@SuppressLint("UnsafeOptInUsageError")
@Composable
internal fun VideoPlayerFullScreenDialog(
    player: ExoPlayer,
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
        PlayerView.switchTargetView(player, fullScreenPlayerView, currentPlayerView)
        currentPlayerView.findViewById<ImageButton>(R.id.exo_fullscreen)
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
        val view = LocalView.current
        LaunchedEffect(Unit) {
            PlayerView.switchTargetView(player, currentPlayerView, fullScreenPlayerView)

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
            VideoPlayerSurface(
                playerView = fullScreenPlayerView,
                player = player,
                videoState = videoState,
                usePlayerController = true,
                autoDispose = false,
                onPipEntered = { onDismissRequest() },
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxSize(),
            )
        }
    }
}
