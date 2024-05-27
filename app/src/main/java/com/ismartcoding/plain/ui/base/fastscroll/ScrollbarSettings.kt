package com.ismartcoding.plain.ui.base.fastscroll

import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.runtime.Stable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Stable
data class ScrollbarSettings(
    val enabled: Boolean = true,
    val alwaysShowScrollbar: Boolean = false,
    val scrollbarPadding: Dp = 0.dp,
    val thumbMinLength: Float = 0.1f,
    val selectionMode: ScrollbarSelectionMode = ScrollbarSelectionMode.Full,
    val selectionActionable: ScrollbarSelectionActionable = ScrollbarSelectionActionable.Always,
    val hideDelayMillis: Int = 2000,
    val hideDisplacement: Dp = 0.dp,
    val hideEasingAnimation: Easing = FastOutSlowInEasing,
    val durationAnimationMillis: Int = 500,
) {
    companion object {
        val Default = ScrollbarSettings()
    }
}

