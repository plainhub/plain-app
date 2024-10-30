package com.ismartcoding.plain.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import com.ismartcoding.plain.ui.theme.palette.dynamicDarkColorScheme
import com.ismartcoding.plain.ui.theme.palette.dynamicLightColorScheme

@Composable
fun AppTheme(
    useDarkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme =
            if (useDarkTheme) dynamicDarkColorScheme()
            else dynamicLightColorScheme(),
        typography = SystemTypography.applyTextDirection(),
        shapes = Shapes,
        content = content,
    )
}
