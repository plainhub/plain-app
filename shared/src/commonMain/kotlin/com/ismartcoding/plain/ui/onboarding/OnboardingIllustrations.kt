package com.ismartcoding.plain.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.devices
import com.ismartcoding.plain.ui.resources.lock
import com.ismartcoding.plain.ui.resources.settings
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource

internal val ink = Color(0xff202124)
internal val accent = Color(0xff424f8b)

@Composable
internal fun OnboardingCanvas(modifier: Modifier = Modifier, scene: DrawScope.() -> Unit) {
    Canvas(modifier.fillMaxWidth().aspectRatio(1.8f).clip(MaterialTheme.shapes.extraLarge)) {
        drawRect(Color(0xfff5f5f7))
        scale(size.width / 720f, pivot = Offset.Zero, block = scene)
    }
}

internal fun DrawScope.panel(left: Float, top: Float, width: Float, height: Float, radius: Float) {
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

internal fun DrawScope.label(
    measurer: TextMeasurer, value: String, x: Float, y: Float, pixels: Float,
    color: Color = ink, mono: Boolean = false, centered: Boolean = false,
): Float {
    val layout = measurer.measure(value, TextStyle(fontSize = pixels.toSp(),
        fontFamily = if (mono) FontFamily.Monospace else FontFamily.SansSerif), softWrap = false)
    val width = layout.size.width.toFloat()
    drawText(layout, color, Offset(if (centered) x - width / 2 else x, y - layout.size.height / 2f))
    return width
}

@Composable
internal fun DirectConnectionIllustration() {
    OnboardingCanvas {
        panel(88f, 100f, 152f, 200f, 20f)
        drawRoundRect(ink, Offset(146f, 115f), Size(36f, 4f), CornerRadius(2f))
        drawPhoto(104f, 146f, 120f, 108f)
        drawLine(accent, Offset(264f, 200f), Offset(333f, 200f), 3f, StrokeCap.Round)
        drawLine(accent, Offset(322f, 190f), Offset(333f, 200f), 3f, StrokeCap.Round)
        drawLine(accent, Offset(322f, 210f), Offset(333f, 200f), 3f, StrokeCap.Round)
        panel(358f, 90f, 274f, 220f, 16f)
        drawLine(Color(0xffdedfe3), Offset(359f, 128f), Offset(631f, 128f))
        drawRoundRect(Color(0xffeceef2), Offset(378f, 105f), Size(234f, 8f), CornerRadius(4f))
        drawPhoto(378f, 146f, 234f, 144f)
    }
}

private fun DrawScope.drawPhoto(left: Float, top: Float, width: Float, height: Float) {
    translate(left, top) {
        drawRoundRect(Color(0xffeff0f6), Offset.Zero, Size(width, height), CornerRadius(8f))
        drawCircle(accent, height * 0.07f, Offset(width * 0.72f, height * 0.28f))
        val ridge = Path().apply {
            moveTo(width * 0.08f, height * 0.75f)
            cubicTo(width * 0.38f, height * 0.29f, width * 0.39f, height * 0.39f, width * 0.58f, height * 0.62f)
            cubicTo(width * 0.7f, height * 0.47f, width * 0.77f, height * 0.57f, width * 0.92f, height * 0.75f)
        }
        drawPath(ridge, accent, style = Stroke(3f, cap = StrokeCap.Round))
    }
}

@Composable
internal fun AccessSettingsIllustration() {
    val title = stringResource(Res.string.access_settings)
    val rows = listOf(
        painterResource(UiRes.drawable.devices) to stringResource(Res.string.connections),
        painterResource(UiRes.drawable.lock) to stringResource(Res.string.security),
        painterResource(UiRes.drawable.settings) to stringResource(Res.string.access_remote_permissions),
    )
    val description = (listOf(title) + rows.map { it.second }).joinToString(". ")
    val text = rememberTextMeasurer()
    OnboardingCanvas(Modifier.semantics { contentDescription = description }) {
        panel(100f, 52f, 520f, 296f, 23f)
        label(text, title, 136f, 100f, 30f)
        rows.forEachIndexed { index, (icon, name) ->
            val y = 168f + 68f * index
            drawLine(Color(0xffe7e8ec), Offset(136f, y - 34f), Offset(584f, y - 34f))
            translate(136f, y - 15f) {
                with(icon) { draw(Size(30f, 30f), colorFilter = ColorFilter.tint(accent)) }
            }
            label(text, name, 190f, y, 24f)
        }
    }
}
