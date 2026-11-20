package com.ismartcoding.plain.ui.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.home_remote_access
import com.ismartcoding.plain.i18n.onboarding_log_in
import org.jetbrains.compose.resources.stringResource
import kotlin.math.PI
import kotlin.math.sin

private const val ADDRESS = "https://192.0.2.20"
private val ink = Color(0xff202124)
private val accent = Color(0xff424f8b)
private val chrome = Color(0xff62656b)

@Composable
internal fun RemoteAccessAnimation(active: Boolean) {
    val lifecycleState by LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
    val time = if (active && lifecycleState == Lifecycle.State.RESUMED) {
        rememberInfiniteTransition(label = "Remote access").animateFloat(
            initialValue = 0f,
            targetValue = 5.8f,
            animationSpec = infiniteRepeatable(tween(5800, easing = LinearEasing)),
            label = "Connection preview",
        )
    } else null
    val title = stringResource(Res.string.home_remote_access)
    val login = stringResource(Res.string.onboarding_log_in)
    val text = rememberTextMeasurer(cacheSize = 32)
    val pointer = remember {
        Path().apply {
            moveTo(0f, 0f)
            lineTo(1f, 24f)
            lineTo(7f, 18f)
            lineTo(13f, 28f)
            lineTo(18f, 25f)
            lineTo(12f, 16f)
            lineTo(22f, 15f)
            close()
        }
    }
    Canvas(Modifier.fillMaxWidth().aspectRatio(1.8f).clip(MaterialTheme.shapes.extraLarge)) {
        drawRect(Color(0xfff5f5f7))
        val seconds = time?.value ?: 0f
        scale(size.width / 720f, pivot = Offset.Zero) {
            if (seconds < 1.98f) drawAccessSwitch(seconds, text, title, pointer)
            else drawBrowser(seconds, text, login)
        }
    }
}

private fun ease(progress: Float): Float {
    val remaining = 1f - progress.coerceIn(0f, 1f)
    return 1f - remaining * remaining * remaining
}

private fun DrawScope.panel(left: Float, top: Float, width: Float, height: Float, radius: Float) {
    for (spread in 8 downTo 1) {
        val extra = spread.toFloat()
        drawRoundRect(ink.copy(alpha = 0.007f), Offset(left - extra, top + 4 - extra),
            Size(width + extra * 2, height + extra * 2), CornerRadius(radius + extra))
    }
    val origin = Offset(left, top)
    val bounds = Size(width, height)
    drawRoundRect(Color.White, origin, bounds, CornerRadius(radius))
    drawRoundRect(Color(0xffdadbe0), origin, bounds, CornerRadius(radius), style = Stroke(1f))
}

private fun DrawScope.label(
    measurer: TextMeasurer, value: String, x: Float, y: Float, pixels: Float,
    color: Color = ink, mono: Boolean = false, centered: Boolean = false,
): Float {
    val layout = measurer.measure(value, TextStyle(fontSize = pixels.toSp(),
        fontFamily = if (mono) FontFamily.Monospace else FontFamily.SansSerif), softWrap = false)
    val width = layout.size.width.toFloat()
    drawText(layout, color, Offset(if (centered) x - width / 2 else x, y - layout.size.height / 2f))
    return width
}

private fun DrawScope.drawAccessSwitch(t: Float, text: TextMeasurer, title: String, pointer: Path) {
    val enabled = ease((t - 0.65f) / 0.27f)
    val expanded = ease((t - 0.78f) / 0.31f)
    val top = 143f - 21 * expanded
    panel(100f, top, 520f, 106 + 53 * expanded, 23f)
    label(text, title, 136f, top + 52, 28f)
    val centerY = top + 53
    val press = sin(PI.toFloat() * ((t - 0.49f) / 0.35f).coerceIn(0f, 1f))
    val radius = 18f - 1.3f * press
    drawRoundRect(lerp(Color(0xffd9dade), Color(0xff34875c), enabled),
        Offset(517f, centerY - radius), Size(62f, radius * 2), CornerRadius(radius))
    drawCircle(Color.White, 14f, Offset(535 + 26 * enabled, centerY))
    if (expanded > 0f) {
        drawLine(Color(0xffe7e8ec).copy(alpha = expanded), Offset(136f, top + 91), Offset(584f, top + 91))
        label(text, ADDRESS, 136f, top + 121, 21f, ink.copy(alpha = expanded), mono = true)
    }
    if (t in 0.16f..1.07f) {
        val approach = ease((t - 0.16f) / 0.4f)
        val leave = ease((t - 0.84f) / 0.23f)
        translate(593 - 40 * approach + 16 * leave, centerY + 52 - 44 * approach + 18 * leave) {
            drawPath(pointer, ink.copy(alpha = 1 - leave))
            drawPath(pointer, Color.White.copy(alpha = 1 - leave), style = Stroke(2f))
        }
    }
}

private fun DrawScope.drawBrowser(t: Float, text: TextMeasurer, login: String) {
    panel(64f, 45f, 592f, 310f, 19f)
    drawLine(Color(0xffe1e2e6), Offset(65f, 112f), Offset(655f, 112f))
    drawLine(chrome, Offset(102f, 71f), Offset(94f, 79f), 2f)
    drawLine(chrome, Offset(94f, 79f), Offset(102f, 87f), 2f)
    val focus = 1 - ease((t - 4.02f) / 0.3f)
    val origin = Offset(126f, 59f)
    val bounds = Size(503f, 41f)
    drawRoundRect(lerp(Color.White, Color(0xfff9faff), focus), origin, bounds, CornerRadius(12f))
    drawRoundRect(lerp(Color(0xffdedfe3), Color(0xff858fb7), focus), origin, bounds, CornerRadius(12f), style = Stroke(1f))
    drawCircle(chrome, 7f, Offset(150f, 79f), style = Stroke(1f))
    drawOval(chrome, Offset(147f, 72f), Size(6f, 14f), style = Stroke(1f))
    drawLine(chrome, Offset(143f, 79f), Offset(157f, 79f))
    val count = (((t - 2.24f) / 1.48f).coerceIn(0f, 1f) * ADDRESS.length).toInt()
    val end = 172 + label(text, ADDRESS.take(count), 170f, 80f, 18f, mono = true)
    if (t < 3.98f) drawLine(accent, Offset(end, 69f), Offset(end, 91f), 2f)
    if (t in 3.99f..4.29f) {
        drawLine(accent, Offset(66f, 112f), Offset(66 + 588 * ease((t - 3.99f) / 0.3f), 112f), 2f)
    }
    if (t >= 4.21f) {
        val shown = ease((t - 4.21f) / 0.32f)
        translate(0f, 5 * (1 - shown)) {
            label(text, "PlainApp", 360f, 199f, 42f, ink.copy(alpha = shown), centered = true)
            drawRoundRect(accent.copy(alpha = shown), Offset(289f, 241f), Size(142f, 42f), CornerRadius(21f))
            label(text, login, 360f, 262f, 17f, Color.White, centered = true)
        }
    }
}
