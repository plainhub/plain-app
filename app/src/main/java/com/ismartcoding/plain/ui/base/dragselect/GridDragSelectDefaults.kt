package com.ismartcoding.plain.ui.base.dragselect

import androidx.compose.runtime.Composable
import androidx.compose.ui.hapticfeedback.HapticFeedback
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp

object GridDragSelectDefaults {

    val hapticsFeedback: HapticFeedback
        @Composable get() = LocalHapticFeedback.current

    val autoScrollThreshold: Float
        @Composable get() = with(LocalDensity.current) { DEFAULT_THRESHOLD_DP.dp.toPx() }

    private const val DEFAULT_THRESHOLD_DP = 40
}
