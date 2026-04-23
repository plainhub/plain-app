package com.ismartcoding.plain.enums

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

enum class ButtonSize(val height: Dp, val cornerRadius: Dp) {
    SMALL(32.dp, 16.dp),
    MEDIUM(48.dp, 24.dp),
    LARGE(56.dp, 28.dp),
}
