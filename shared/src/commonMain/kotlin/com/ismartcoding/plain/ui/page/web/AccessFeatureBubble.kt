package com.ismartcoding.plain.ui.page.web

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.stringResource
import kotlin.math.roundToInt

private val BUBBLE_ARROW_SIZE = 12.dp
private const val AUTO_DISMISS_MS = 8000L

/**
 * Coach-mark bubble floating over the page: an arrow points at the highlighted
 * option's [anchor] (window coords). Non-blocking, so the user can still reach
 * the switch underneath; dismisses on tap or after a timeout.
 */
@Composable
internal fun AccessFeatureBubble(
    anchor: Rect,
    overlayOrigin: Offset,
    overlaySize: IntSize,
    label: String,
    onDismiss: () -> Unit,
) {
    var contentSize by remember { mutableStateOf(IntSize.Zero) }
    LaunchedEffect(anchor) {
        delay(AUTO_DISMISS_MS)
        onDismiss()
    }
    val arrow = with(LocalDensity.current) { BUBBLE_ARROW_SIZE.roundToPx() }
    val margin = with(LocalDensity.current) { 8.dp.roundToPx() }
    val anchorCenterX = (anchor.left + anchor.width / 2).roundToInt() - overlayOrigin.x.roundToInt()
    val anchorTop = anchor.top.roundToInt() - overlayOrigin.y.roundToInt()
    val anchorBottom = anchor.bottom.roundToInt() - overlayOrigin.y.roundToInt()
    // Prefer above the row; flip below when there is no room (row near top).
    val above = anchorTop >= contentSize.height + arrow + with(LocalDensity.current) { 56.dp.roundToPx() }
    val x = (anchorCenterX - contentSize.width / 2)
        .coerceIn(margin, (overlaySize.width - contentSize.width - margin).coerceAtLeast(margin))
    val y = if (above) anchorTop - arrow - contentSize.height else anchorBottom + arrow
    val arrowShiftRange = with(LocalDensity.current) { (contentSize.width / 2 - BUBBLE_ARROW_SIZE.roundToPx()).coerceAtLeast(0) }
    val arrowShift = (anchorCenterX - x - contentSize.width / 2).coerceIn(-arrowShiftRange, arrowShiftRange)
    Column(
        modifier = Modifier
            .offset { IntOffset(x, y) }
            .alpha(if (contentSize == IntSize.Zero) 0f else 1f)
            .onGloballyPositioned { contentSize = it.size },
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (!above) BubbleArrow(pointDown = false, shift = arrowShift)
        Surface(
            onClick = onDismiss,
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            shadowElevation = 6.dp,
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
    val color = MaterialTheme.colorScheme.primaryContainer
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
