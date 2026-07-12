package com.ismartcoding.plain.platform

import android.view.HapticFeedbackConstants
import android.view.SoundEffectConstants
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView

@Composable
actual fun rememberClickFeedback(): (isHaptic: Boolean, isSound: Boolean) -> Unit {
    val view = LocalView.current
    return remember(view) {
        { isHaptic: Boolean, isSound: Boolean ->
            if (isHaptic) view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
            if (isSound) view.playSoundEffect(SoundEffectConstants.CLICK)
        }
    }
}
