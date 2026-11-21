package com.ismartcoding.plain.ui.components

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class PSwitchColorTest {
    @Test
    fun uncheckedThumbRemainsVisibleOnTheDarkTrack() {
        val colors = darkColorScheme(
            outline = Color(0xFF38383A),
            onSurfaceVariant = Color(0xFF8D8D93),
            surfaceContainerHighest = Color(0xFF333440),
        )
        val contrast = (colors.uncheckedSwitchThumb(darkTheme = true).luminance() + 0.05f) /
            (colors.surfaceContainerHighest.luminance() + 0.05f)
        assertTrue(contrast >= 3f, "Off thumb contrast: $contrast")
    }

    @Test
    fun lightThemeKeepsTheDefaultThumb() {
        assertEquals(Color.Unspecified, lightColorScheme().uncheckedSwitchThumb(darkTheme = false))
    }
}
