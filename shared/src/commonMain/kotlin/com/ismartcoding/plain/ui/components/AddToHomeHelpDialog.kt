package com.ismartcoding.plain.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.VerticalSpace
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import kotlin.math.min

// Help entry for "Add to Home Screen": a trailing help icon that opens a
// dialog with a looping animation showing a shortcut landing on the phone's
// home screen, so users understand "Home Screen" means the phone launcher.

@Composable
fun AddToHomeHelpAction() {
    var showHelp by remember { mutableStateOf(false) }
    IconButton(onClick = { showHelp = true }, modifier = Modifier.size(32.dp)) {
        Icon(
            painter = painterResource(Res.drawable.circle_help),
            contentDescription = stringResource(Res.string.help),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp),
        )
    }
    if (showHelp) {
        AddToHomeHelpDialog(onDismiss = { showHelp = false })
    }
}

@Composable
fun AddToHomeHelpDialog(onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text(
                text = stringResource(Res.string.home_screen_help_title),
                style = MaterialTheme.typography.titleLarge,
            )
        },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                HomeScreenDemo(Modifier.fillMaxWidth().height(230.dp))
                VerticalSpace(16.dp)
                Text(
                    text = stringResource(Res.string.home_screen_help_text),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(Res.string.ok)) }
        },
    )
}

@Composable
private fun HomeScreenDemo(modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "addToHomeHelp")
    val p by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = 3400, easing = LinearEasing)),
        label = "progress",
    )
    val outline = MaterialTheme.colorScheme.outline
    val placeholder = MaterialTheme.colorScheme.surfaceVariant
    val slot = MaterialTheme.colorScheme.primaryContainer
    val accent = MaterialTheme.colorScheme.primary
    val onAccent = MaterialTheme.colorScheme.onPrimary
    val labelColor = MaterialTheme.colorScheme.onSurfaceVariant

    Canvas(modifier) {
        drawPhoneDemo(p, outline, placeholder, slot, accent, onAccent, labelColor)
    }
}

// Loop timeline (p in [0, 1)): 0-0.30 shortcut drops from above onto a home
// screen slot, 0.30-0.45 landing pulse, 0.45-0.82 settled with app label,
// 0.82-1.00 fade out and restart.
private fun DrawScope.drawPhoneDemo(
    p: Float,
    outline: Color,
    placeholder: Color,
    slot: Color,
    accent: Color,
    onAccent: Color,
    labelColor: Color,
) {
    val pw = min(size.height * 0.46f, size.width * 0.5f)
    val ph = pw * 2.05f
    val left = (size.width - pw) / 2f
    val top = (size.height - ph) / 2f
    drawRoundRect(
        color = outline,
        topLeft = Offset(left, top),
        size = Size(pw, ph),
        cornerRadius = CornerRadius(pw * 0.14f),
        style = Stroke(width = 1.5.dp.toPx()),
    )

    val screenInset = pw * 0.07f
    val sLeft = left + screenInset
    val sTop = top + screenInset
    val sWidth = pw - screenInset * 2f
    val sHeight = ph - screenInset * 2f
    drawCircle(outline, radius = pw * 0.018f, center = Offset(left + pw / 2f, sTop + pw * 0.045f))

    val icon = sWidth * 0.2f
    val gapX = (sWidth - 3 * icon) / 4f
    val gridTop = sTop + sHeight * 0.14f
    val rowStep = icon * 1.55f
    val targetRow = 1
    val targetCol = 1

    fun slotCenter(row: Int, col: Int) = Offset(
        sLeft + gapX * (col + 1) + icon * col + icon / 2f,
        gridTop + row * rowStep + icon / 2f,
    )

    for (r in 0 until 4) {
        for (c in 0 until 3) {
            if (r == targetRow && c == targetCol) continue
            drawAppIconBody(slotCenter(r, c), icon, placeholder, 1f)
        }
    }
    val target = slotCenter(targetRow, targetCol)
    drawAppIconBody(target, icon, slot, 1f)

    val dockIcon = icon * 0.9f
    val dockY = sTop + sHeight - dockIcon / 2f - sHeight * 0.03f
    val dockGap = (sWidth - 4 * dockIcon) / 5f
    for (c in 0 until 4) {
        drawAppIconBody(
            Offset(sLeft + dockGap * (c + 1) + dockIcon * c + dockIcon / 2f, dockY),
            dockIcon,
            placeholder,
            1f,
        )
    }

    val flyT = FastOutSlowInEasing.transform((p / 0.30f).coerceIn(0f, 1f))
    val pulseT = ((p - 0.30f) / 0.15f).coerceIn(0f, 1f)
    val fadeT = ((p - 0.82f) / 0.18f).coerceIn(0f, 1f)
    val iconAlpha = (flyT * 4f).coerceIn(0f, 1f) * (1f - fadeT)
    if (iconAlpha > 0f) {
        val start = Offset(target.x, sTop - icon * 1.4f)
        val center = Offset(start.x, start.y + (target.y - start.y) * flyT)
        drawAppIconBody(center, icon, accent, iconAlpha)
        drawPlayGlyph(center, icon * 0.42f, onAccent, iconAlpha)
        val labelAlpha = pulseT.coerceIn(0f, 1f) * (1f - fadeT)
        if (labelAlpha > 0f) {
            val labelW = icon * 0.9f
            drawRoundRect(
                color = labelColor,
                topLeft = Offset(center.x - labelW / 2f, center.y + icon * 0.85f),
                size = Size(labelW, icon * 0.12f),
                cornerRadius = CornerRadius(icon * 0.06f),
                alpha = labelAlpha * 0.7f,
            )
        }
    }
    if (pulseT > 0f && pulseT < 1f) {
        drawCircle(
            accent,
            radius = icon * (0.75f + 0.7f * pulseT),
            center = target,
            style = Stroke(width = 2.dp.toPx()),
            alpha = (1f - pulseT) * 0.9f,
        )
    }
}

private fun DrawScope.drawAppIconBody(center: Offset, iconPx: Float, color: Color, alpha: Float) {
    drawRoundRect(
        color = color,
        topLeft = Offset(center.x - iconPx / 2f, center.y - iconPx / 2f),
        size = Size(iconPx, iconPx),
        cornerRadius = CornerRadius(iconPx * 0.28f),
        alpha = alpha,
    )
}

private fun DrawScope.drawPlayGlyph(center: Offset, glyph: Float, color: Color, alpha: Float) {
    val path = Path().apply {
        moveTo(center.x - glyph * 0.45f, center.y - glyph * 0.6f)
        lineTo(center.x + glyph * 0.65f, center.y)
        lineTo(center.x - glyph * 0.45f, center.y + glyph * 0.6f)
        close()
    }
    drawPath(path, color = color, alpha = alpha)
}