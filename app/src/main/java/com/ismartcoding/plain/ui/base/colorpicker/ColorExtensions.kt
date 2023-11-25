package com.ismartcoding.plain.ui.base.colorpicker

import androidx.compose.ui.graphics.Color
import java.util.Locale

internal val Color.hexCode: String
  inline get() {
    val a: Int = (alpha * 255).toInt()
    val r: Int = (red * 255).toInt()
    val g: Int = (green * 255).toInt()
    val b: Int = (blue * 255).toInt()
    return java.lang.String.format(Locale.getDefault(), "%02X%02X%02X%02X", a, r, g, b)
  }
