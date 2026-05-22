package com.ismartcoding.plain.preferences

import androidx.compose.runtime.compositionLocalOf
import com.ismartcoding.plain.enums.DarkTheme

val LocalDarkTheme = compositionLocalOf { DarkTheme.UseDeviceTheme.value }
val LocalAmoledDarkTheme = compositionLocalOf { false }
