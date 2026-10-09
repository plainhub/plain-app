package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Immutable

@Immutable
data class WaveOptions(
    val amplitude: Float = 6f,
    val frequency: Float = 0.12f,
    val lineWidth: Float = 3f,
    val thumbRadius: Float = 5f,
    val animationDurationMs: Int = 2000
)
