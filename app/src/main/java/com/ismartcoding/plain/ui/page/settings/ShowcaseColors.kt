package com.ismartcoding.plain.ui.page.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.base.VerticalSpace
import com.ismartcoding.plain.ui.theme.*

@Composable
fun ShowcaseColors() {
    val cs = MaterialTheme.colorScheme
    SectionTitle("Custom Colors (All Used)")
    ColorGrid(
        listOf(
            "green" to cs.green, "grey" to cs.grey,
            "red" to cs.red, "blue" to cs.blue,
            "yellow" to cs.yellow, "orange" to cs.orange,
            "backgroundNormal" to cs.backgroundNormal,
            "cardBackgroundNormal" to cs.cardBackgroundNormal,
            "cardBackgroundActive" to cs.cardBackgroundActive,
            "circleBackground" to cs.circleBackground,
            "secondaryTextColor" to cs.secondaryTextColor,
            "waveActiveColor" to cs.waveActiveColor,
            "waveInactiveColor" to cs.waveInactiveColor,
            "waveThumbColor" to cs.waveThumbColor,
            "badgeBorderColor" to cs.badgeBorderColor,
            "lightMask" to cs.lightMask(),
            "darkMask" to cs.darkMask(),
        ),
    )
    VerticalSpace(8.dp)
    SectionTitle("Material3 Colors (Used in App)")
    ColorGrid(
        listOf(
            "primary" to cs.primary, "onPrimary" to cs.onPrimary,
            "primaryContainer" to cs.primaryContainer,
            "secondary" to cs.secondary, "onSecondary" to cs.onSecondary,
            "secondaryContainer" to cs.secondaryContainer,
            "tertiary" to cs.tertiary,
            "error" to cs.error,
            "errorContainer" to cs.errorContainer,
            "onErrorContainer" to cs.onErrorContainer,
            "surface" to cs.surface, "onSurface" to cs.onSurface,
            "surfaceVariant" to cs.surfaceVariant,
            "onSurfaceVariant" to cs.onSurfaceVariant,
            "background" to cs.background, "onBackground" to cs.onBackground,
            "inverseOnSurface" to cs.inverseOnSurface,
            "outline" to cs.outline, "outlineVariant" to cs.outlineVariant,
        ),
    )
    VerticalSpace(8.dp)
    SectionTitle("Material3 Colors (Theme Only)")
    ColorGrid(
        listOf(
            "onPrimaryContainer" to cs.onPrimaryContainer,
            "inversePrimary" to cs.inversePrimary,
            "onSecondaryContainer" to cs.onSecondaryContainer,
            "onTertiary" to cs.onTertiary,
            "tertiaryContainer" to cs.tertiaryContainer,
            "onTertiaryContainer" to cs.onTertiaryContainer,
            "onError" to cs.onError,
            "surfaceTint" to cs.surfaceTint,
            "inverseSurface" to cs.inverseSurface,
            "scrim" to cs.scrim,
            "surfaceBright" to cs.surfaceBright,
            "surfaceDim" to cs.surfaceDim,
            "surfaceContainer" to cs.surfaceContainer,
            "surfaceContainerLowest" to cs.surfaceContainerLowest,
            "surfaceContainerLow" to cs.surfaceContainerLow,
            "surfaceContainerHigh" to cs.surfaceContainerHigh,
            "surfaceContainerHighest" to cs.surfaceContainerHighest,
        ),
    )
    VerticalSpace(16.dp)
}

@Composable
private fun ColorGrid(colors: List<Pair<String, Color>>) {
    Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
        for (row in colors.chunked(2)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                for ((name, color) in row) {
                    ColorSwatch(name = name, color = color, modifier = Modifier.weight(1f))
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun ColorSwatch(name: String, color: Color, modifier: Modifier = Modifier) {
    Row(modifier = modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .background(color, RoundedCornerShape(6.dp))
                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(6.dp)),
        )
        Text(
            text = name,
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 8.dp),
            maxLines = 1,
        )
    }
}
