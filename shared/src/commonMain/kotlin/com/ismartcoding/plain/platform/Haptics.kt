package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable

@Composable
expect fun rememberClickFeedback(): (isHaptic: Boolean, isSound: Boolean) -> Unit
