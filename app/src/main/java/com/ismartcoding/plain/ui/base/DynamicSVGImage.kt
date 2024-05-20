package com.ismartcoding.plain.ui.base

import android.graphics.drawable.PictureDrawable
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.IntSize
import coil3.compose.AsyncImage
import com.caverock.androidsvg.SVG
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.preference.LocalDarkTheme
import com.ismartcoding.plain.ui.base.svg.parseDynamicColor
import com.ismartcoding.plain.ui.theme.palette.LocalTonalPalettes

@Composable
fun DynamicSVGImage(
    modifier: Modifier = Modifier,
    svgImageString: String,
    contentDescription: String,
) {
    val useDarkTheme = DarkTheme.isDarkTheme(LocalDarkTheme.current)
    val tonalPalettes = LocalTonalPalettes.current
    var size by remember { mutableStateOf(IntSize.Zero) }
    val pic by remember(useDarkTheme, tonalPalettes, size) {
        mutableStateOf(
            PictureDrawable(
                SVG.getFromString(svgImageString.parseDynamicColor(tonalPalettes, useDarkTheme))
                    .renderToPicture(size.width, size.height),
            ),
        )
    }

    Row(
        modifier =
            modifier
                .aspectRatio(1.38f)
                .onGloballyPositioned {
                    if (it.size != IntSize.Zero) {
                        size = it.size
                    }
                },
    ) {
        Crossfade(targetState = pic, label = "") {
            AsyncImage(
                model = it,
                contentDescription = contentDescription,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
