package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class PSegmentedButtonsColors(
    val containerColor: Color,
    val selectedContainerColor: Color,
    val contentColor: Color,
    val selectedContentColor: Color,
)
