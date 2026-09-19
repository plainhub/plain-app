package com.ismartcoding.plain.ui.page.scan.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import com.ismartcoding.plain.platform.ScannedCode
import kotlin.math.roundToInt

/**
 * Tappable markers for a frozen multi-code camera frame: one green circular tag
 * centered on each detected code, in first-detection order.
 */
@Composable
fun BoxScope.ScanCodeTags(
    codes: List<ScannedCode>,
    onPick: (ScannedCode) -> Unit,
) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val density = LocalDensity.current
        val containerW = with(density) { maxWidth.toPx() }
        val containerH = with(density) { maxHeight.toPx() }
        val half = scanCodeTagRadiusPx(density)
        codes.forEachIndexed { index, code ->
            ScanCodeTag(
                index = index,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset {
                        IntOffset(
                            (code.centerX * containerW).roundToInt() - half.roundToInt(),
                            (code.centerY * containerH).roundToInt() - half.roundToInt(),
                        )
                    },
                onPick = { onPick(code) },
            )
        }
    }
}
