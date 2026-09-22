package com.ismartcoding.plain.ui.page.audio.components

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import kotlin.math.abs

/** Soft cover gradients rotated by a stable index (light theme, e-ink friendly). */
private val AudioHomeGradients = listOf(
    listOf(Color(0xFFDDE2F9), Color(0xFFE8DEF8)),
    listOf(Color(0xFFE5F0FF), Color(0xFFDDE2F9)),
    listOf(Color(0xFFE8DEF8), Color(0xFFF2E7EF)),
    listOf(Color(0xFFDFF0E4), Color(0xFFDDE2F9)),
    listOf(Color(0xFFFFF0DC), Color(0xFFE8DEF8)),
    listOf(Color(0xFFDDE2F9), Color(0xFFDFF0E4)),
)

fun audioHomeGradientAt(index: Int): Brush {
    val colors = AudioHomeGradients[abs(index) % AudioHomeGradients.size]
    return Brush.linearGradient(colors)
}

/** Stable gradient index for a path so covers keep their color across screens. */
fun audioHomeGradientIndexOf(path: String): Int = abs(path.hashCode()) % AudioHomeGradients.size
