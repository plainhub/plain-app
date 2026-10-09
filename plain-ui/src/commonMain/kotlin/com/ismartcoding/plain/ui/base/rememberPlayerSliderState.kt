package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember

@Composable
fun rememberPlayerSliderState(initialProgress: Float = 0f): PlayerSliderState =
    remember { PlayerSliderState(initialProgress) }
