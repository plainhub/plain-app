package com.ismartcoding.plain.ui.base

import androidx.compose.runtime.compositionLocalOf

val LocalOpenDrawer = compositionLocalOf<(() -> Unit)?> { null }
