package com.ismartcoding.plain.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import com.ismartcoding.plain.preference.LocalThemeIndex
import com.ismartcoding.plain.ui.theme.palette.LocalTonalPalettes
import com.ismartcoding.plain.ui.theme.palette.TonalPalettes
import com.ismartcoding.plain.ui.theme.palette.core.ProvideZcamViewingConditions
import com.ismartcoding.plain.ui.theme.palette.dynamic.extractTonalPalettesFromUserWallpaper
import com.ismartcoding.plain.ui.theme.palette.dynamicDarkColorScheme
import com.ismartcoding.plain.ui.theme.palette.dynamicLightColorScheme

@Composable
fun AppTheme(
    useDarkTheme: Boolean,
    wallpaperPalettes: List<TonalPalettes> = extractTonalPalettesFromUserWallpaper(),
    content: @Composable () -> Unit,
) {
    val themeIndex = LocalThemeIndex.current

    val tonalPalettes = wallpaperPalettes[
        if (themeIndex >= wallpaperPalettes.size) {
            when {
                wallpaperPalettes.size == 5 -> 0
                wallpaperPalettes.size > 5 -> 5
                else -> 0
            }
        } else {
            themeIndex
        }
    ]

    ProvideZcamViewingConditions {
        CompositionLocalProvider(
            LocalTonalPalettes provides tonalPalettes.apply { Preparing() },
            LocalTextStyle provides LocalTextStyle.current.applyTextDirection()
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
    }
}
