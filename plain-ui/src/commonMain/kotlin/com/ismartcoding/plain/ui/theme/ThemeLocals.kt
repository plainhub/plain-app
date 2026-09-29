package com.ismartcoding.plain.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
val LocalDarkTheme = staticCompositionLocalOf { false }
val LocalAmoledDarkTheme = staticCompositionLocalOf { false }

@Composable
fun PlainUiTheme(
    darkTheme: Boolean,
    amoledDarkTheme: Boolean = false,
    content: @Composable () -> Unit,
) {
    CompositionLocalProvider(
        LocalDarkTheme provides darkTheme,
        LocalAmoledDarkTheme provides amoledDarkTheme,
        content = content,
    )
}
