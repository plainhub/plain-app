package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable

@Composable
actual fun rememberClickFeedback(): (isHaptic: Boolean, isSound: Boolean) -> Unit {
    return { _, _ -> }
}
