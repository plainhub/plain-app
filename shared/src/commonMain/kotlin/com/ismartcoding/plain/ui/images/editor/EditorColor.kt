package com.ismartcoding.plain.ui.images.editor

import androidx.compose.ui.graphics.toArgb
import com.ismartcoding.plain.ui.images.editor.scene.parseScopeBgColor

internal fun editorColorToPicker(value: String): String = parseScopeBgColor(value).toArgb().toUInt().toString(16).padStart(8, '0').uppercase()

internal fun pickerColorToEditor(value: String): String {
    require(value.length == 8 && value.all { it.digitToIntOrNull(16) != null }) { "Enter eight hexadecimal ARGB digits" }
    val argb = value.lowercase()
    return "#" + argb.substring(2) + if (argb.startsWith("ff")) "" else argb.take(2)
}
