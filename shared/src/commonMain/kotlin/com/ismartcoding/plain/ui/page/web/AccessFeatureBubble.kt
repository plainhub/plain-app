package com.ismartcoding.plain.ui.page.web

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private val BUBBLE_ARROW_SIZE = 12.dp

/**
 * Coach-mark bubble floating over the page: [anchor] (window coords) is the
 * highlighted row used for vertical placement, [switchAnchor] the row's switch
 * the arrow points at. Eye-catching by design: accent color + gentle pulse;
 * stays until the user taps it.
 */
@Composable
internal fun AccessFeatureBubble(
    anchor: Rect,
    switchAnchor: Rect?,
    overlayOrigin: Offset,
    overlaySize: IntSize,
    label: String,
    onDismiss: () -> Unit,
) {
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    val pulse = rememberInfiniteTransition(label = "accessFeatureBubble")
    val scale by pulse.animateFloat(
        initialValue = 1f,
        targetValue = 1.04f,
        animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
        label = "bubbleScale",
    )
    val arrow = with(LocalDensity.current) { BUBBLE_ARROW_SIZE.roundToPx() }
    val margin = with(LocalDensity.current) { 8.dp.roundToPx() }
    val target = switchAnchor ?: anchor
    val targetCenterX = (target.left + target.width / 2).roundToInt() - overlayOrigin.x.roundToInt()
    val anchorTop = anchor.top.roundToInt() - overlayOrigin.y.roundToInt()
    val anchorBottom = anchor.bottom.roundToInt() - overlayOrigin.y.roundToInt()
    // Prefer above the row; flip below when there is no room (row near top).
    val above = anchorTop >= contentSize.height + arrow + with(LocalDensity.current) { 56.dp.roundToPx() }
    val x = (targetCenterX - contentSize.width / 2)
        .coerceIn(margin, (overlaySize.width - contentSize.width - margin).coerceAtLeast(margin))
    val y = if (above) anchorTop - arrow - contentSize.height else anchorBottom + arrow
    val arrowShiftRange = with(LocalDensity.current) { (contentSize.width / 2 - BUBBLE_ARROW_SIZE.roundToPx()).coerceAtLeast(0) }
    val arrowShift = (targetCenterX - x - contentSize.width / 2).coerceIn(-arrowShiftRange, arrowShiftRange)
    Column(
        modifier = Modifier
            .offset { IntOffset(x, y) }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (contentSize == IntSize.Zero) 0f else 1f
            }
            .onGloballyPositioned { contentSize = it.size },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!above) BubbleArrow(pointDown = false, shift = arrowShift)
        Surface(
            onClick = onDismiss,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
            shadowElevation = 12.dp,
        ) {
            Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp).widthIn(max = 280.dp)) {
                Text(label, style = MaterialTheme.typography.titleSmall)
                Text(stringResource(Res.string.access_feature_hint), style = MaterialTheme.typography.bodySmall)
            }
        }
        if (above) BubbleArrow(pointDown = true, shift = arrowShift)
    }
}

@Composable
private fun BubbleArrow(pointDown: Boolean, shift: Int) {
    val color = MaterialTheme.colorScheme.primary
    Canvas(
        Modifier
            .offset { IntOffset(shift, 0) }
            .size(16.dp, BUBBLE_ARROW_SIZE)
    ) {
        val path = Path()
        if (pointDown) {
            path.moveTo(0f, 0f)
            path.lineTo(size.width, 0f)
            path.lineTo(size.width / 2, size.height)
        } else {
            path.moveTo(0f, size.height)
            path.lineTo(size.width, size.height)
            path.lineTo(size.width / 2, 0f)
        }
        path.close()
        drawPath(path, color)
    }
}
