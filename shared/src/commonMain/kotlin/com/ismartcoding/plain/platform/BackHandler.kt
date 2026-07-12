package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable

@Composable
expect fun PBackHandler(enabled: Boolean = true, onBack: () -> Unit)
