package com.ismartcoding.plain.ui.base.pullrefresh

import androidx.compose.foundation.gestures.Orientation

enum class ComposePosition(val orientation: Orientation) {
    Start(Orientation.Horizontal),
    End(Orientation.Horizontal),
    Top(Orientation.Vertical),
    Bottom(Orientation.Vertical),
    ;

    fun isHorizontal(): Boolean = orientation == Orientation.Horizontal
}
