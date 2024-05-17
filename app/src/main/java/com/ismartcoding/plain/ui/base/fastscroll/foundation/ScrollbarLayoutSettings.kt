package com.ismartcoding.plain.ui.base.fastscroll.foundation

import androidx.compose.animation.core.Easing
import androidx.compose.runtime.Stable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import com.ismartcoding.plain.ui.base.fastscroll.ScrollbarLayoutSide
import com.ismartcoding.plain.ui.base.fastscroll.ScrollbarSelectionActionable

@Stable
internal data class ScrollbarLayoutSettings(
    val scrollbarPadding: Dp,
    val thumbShape: Shape,
    val thumbThickness: Dp,
    val side: ScrollbarLayoutSide,
    val selectionActionable: ScrollbarSelectionActionable,
    val hideDisplacement: Dp,
    val hideDelayMillis: Int,
    val hideEasingAnimation: Easing,
    val durationAnimationMillis: Int
)