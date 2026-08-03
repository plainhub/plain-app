package com.ismartcoding.plain.ui.page.home

import com.ismartcoding.plain.i18n.*

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.platform.applySystemBarAppearanceForDarkTheme
import com.ismartcoding.plain.platform.keepScreenOn
import com.ismartcoding.plain.platform.setImmersiveFullscreen
import com.ismartcoding.plain.preferences.LocalDarkTheme
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun StayOnlineModeOverlay(onExit: () -> Unit) {
    val scope = rememberCoroutineScope()

    // true = pure black screen; false = text visible
    val totalSeconds = 30
    var sleeping by remember { mutableStateOf(false) }
    var remainingSeconds by remember { mutableStateOf(totalSeconds) }
    var sleepJob by remember { mutableStateOf<Job?>(null) }

    fun scheduleSleep(seconds: Int) {
        sleepJob?.cancel()
        remainingSeconds = seconds
        sleepJob = scope.launch {
            while (remainingSeconds > 0) {
                delay(1_000)
                remainingSeconds -= 1
            }
            sleeping = true
        }
    }

    val currentUseDarkTheme by rememberUpdatedState(DarkTheme.isDarkTheme(LocalDarkTheme.current))

    DisposableEffect(Unit) {
        keepScreenOn(true)
        onDispose {
            keepScreenOn(false)
            applySystemBarAppearanceForDarkTheme(currentUseDarkTheme)
        }
    }

    // Initial display: countdown 30s then sleep
    LaunchedEffect(Unit) { scheduleSleep(totalSeconds) }

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
        setImmersiveFullscreen()

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
                        scheduleSleep(totalSeconds)
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
                        text = stringResource(Res.string.stay_online_keep_running),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 20.sp,
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                    Text(
                        text = stringResource(Res.string.stay_online_screen_black_countdown, remainingSeconds),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 20.sp,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(Res.string.stay_online_tap_to_exit),
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 20.sp,
                    )
                }
            }
        }
    }
}
