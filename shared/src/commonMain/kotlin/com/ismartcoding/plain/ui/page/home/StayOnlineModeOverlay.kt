package com.ismartcoding.plain.ui.page.home

import com.ismartcoding.plain.i18n.*

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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import org.jetbrains.compose.resources.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.ismartcoding.plain.platform.exitImmersiveFullscreen
import com.ismartcoding.plain.platform.keepScreenOn
import com.ismartcoding.plain.platform.setImmersiveFullscreen
import com.ismartcoding.plain.ui.base.POutlinedButton

@Composable
fun StayOnlineModeOverlay(onExit: () -> Unit) {
    // true = pure black screen; false = text visible
    var sleeping by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        keepScreenOn(true)
        onDispose {
            keepScreenOn(false)
            exitImmersiveFullscreen()
        }
    }

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
                        // Wake on tap so the user can reach the controls again.
                        sleeping = false
                    } else {
                        onExit()
                    }
                },
            contentAlignment = Alignment.Center,
        ) {
            if (!sleeping) {
                val softWhite = Color.White.copy(alpha = 0.7f)
                Column(
                    modifier = Modifier.padding(horizontal = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.stay_online_keep_running),
                        color = softWhite,
                        fontSize = 20.sp,
                    )
                    Spacer(modifier = Modifier.height(40.dp))
                    POutlinedButton(
                        text = stringResource(Res.string.stay_online_go_dark_now),
                        onClick = { sleeping = true },
                        contentColor = softWhite,
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = stringResource(Res.string.stay_online_tap_to_exit),
                        color = softWhite,
                        fontSize = 20.sp,
                    )
                }
            }
        }
    }
}
