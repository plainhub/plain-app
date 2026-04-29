package com.ismartcoding.plain.ui.page.home

import android.app.Activity
import android.view.WindowManager
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit.Companion
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.ismartcoding.plain.R
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StayOnlineModeOverlay(onExit: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? Activity
    val scope = rememberCoroutineScope()

    // true = pure black screen; false = text visible
    var sleeping by remember { mutableStateOf(false) }
    var sleepJob by remember { mutableStateOf<Job?>(null) }

    fun scheduleSleep(delayMs: Long) {
        sleepJob?.cancel()
        sleepJob = scope.launch {
            delay(delayMs)
            sleeping = true
        }
    }

    DisposableEffect(Unit) {
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose {
            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        }
    }

    // Initial display: show text for 3s then sleep
    LaunchedEffect(Unit) { scheduleSleep(3_000) }

    val textAlpha by animateFloatAsState(
        targetValue = if (sleeping) 0f else 1f,
        animationSpec = tween(durationMillis = 1500),
        label = "textAlpha",
    )

    Dialog(
        onDismissRequest = onExit,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = true,
            dismissOnClickOutside = false,
        ),
    ) {
        val dialogView = LocalView.current
        SideEffect {
            val dialogWindow = (dialogView.parent as? DialogWindowProvider)?.window
            if (dialogWindow != null) {
                WindowCompat.setDecorFitsSystemWindows(dialogWindow, false)
                WindowInsetsControllerCompat(dialogWindow, dialogWindow.decorView).apply {
                    hide(WindowInsetsCompat.Type.systemBars())
                    systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) {
                    if (sleeping) {
                        sleeping = false
                        scheduleSleep(5_000)
                    } else {
                        onExit()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (!sleeping) {
                Column(
                    modifier = Modifier
                        .alpha(textAlpha)
                        .padding(horizontal = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.stay_online_mode),
                        color = Color.White.copy(alpha = 0.8f),
                        lineHeight = 56.sp,
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.stay_online_mode_oled_saving),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 20.sp,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(R.string.stay_online_mode_tap_to_exit),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 20.sp,
                    )
                }
            }
        }
    }
}
