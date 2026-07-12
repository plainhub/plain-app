package com.ismartcoding.plain.ui.page.cast

import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke

internal fun DrawScope.drawMonitor(monLeft: Float, monTop: Float, monW: Float, monH: Float) {
    drawRoundRect(
        color = Color(0xFFF2F4F7),
        topLeft = Offset(monLeft, monTop),
        size = Size(monW, monH),
        cornerRadius = CornerRadius(10f),
    )
    drawRoundRect(
        color = Color(0xFFCDD2D9),
        topLeft = Offset(monLeft, monTop),
        size = Size(monW, monH),
        cornerRadius = CornerRadius(10f),
        style = Stroke(width = 2f),
    )
    val standCx = monLeft + monW / 2f
    val neckW = monW * 0.05f
    val neckH = monH * 0.10f
    drawRoundRect(
        color = Color(0xFFCDD2D9),
        topLeft = Offset(standCx - neckW / 2f, monTop + monH),
        size = Size(neckW, neckH),
        cornerRadius = CornerRadius(3f),
    )
    val baseW = monW * 0.22f
    val baseH = neckH * 0.35f
    drawRoundRect(
        color = Color(0xFFCDD2D9),
        topLeft = Offset(standCx - baseW / 2f, monTop + monH + neckH - baseH / 2f),
        size = Size(baseW, baseH),
        cornerRadius = CornerRadius(3f),
    )
}

internal fun DrawScope.drawPhone(phoneLeft: Float, phoneTop: Float, phoneW: Float, phoneH: Float) {
    drawRoundRect(
        color = Color(0xFF1A73E8),
        topLeft = Offset(phoneLeft, phoneTop),
        size = Size(phoneW, phoneH),
        cornerRadius = CornerRadius(14f),
    )
    val topBezel = phoneH * 0.06f
    val sideInset = phoneW * 0.08f
    drawRoundRect(
        color = Color(0x33FFFFFF),
        topLeft = Offset(phoneLeft + sideInset, phoneTop + topBezel),
        size = Size(phoneW - sideInset * 2, phoneH - topBezel - phoneH * 0.04f),
        cornerRadius = CornerRadius(6f),
    )
    val camW = phoneW * 0.18f
    val camH = topBezel * 0.45f
    drawRoundRect(
        color = Color(0x66FFFFFF),
        topLeft = Offset(phoneLeft + (phoneW - camW) / 2f, phoneTop + topBezel * 0.28f),
        size = Size(camW, camH),
        cornerRadius = CornerRadius(camH / 2f),
    )
}

internal fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t.coerceIn(0f, 1f)
