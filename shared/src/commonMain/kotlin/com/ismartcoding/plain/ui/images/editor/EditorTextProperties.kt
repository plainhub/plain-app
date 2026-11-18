package com.ismartcoding.plain.ui.images.editor

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.yjs.EditorFontStyle
import com.ismartcoding.plain.lib.yjs.EditorFontWeight
import com.ismartcoding.plain.lib.yjs.EditorTextAlign
import com.ismartcoding.plain.lib.yjs.EditorTextDecoration
import com.ismartcoding.plain.lib.yjs.TextLayer
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun EditorTextProperties(layer: TextLayer, update: (TextLayer) -> Unit) {
    EditorNumberField(stringResource(Res.string.image_editor_font_size), layer.fontSize, 1.0..500.0) { update(layer.copy(fontSize = it)) }
    EditorChoices(stringResource(Res.string.image_editor_font_family), layer.fontFamily, listOf("sans-serif" to "Sans", "serif" to "Serif", "monospace" to "Mono", "cursive" to "Cursive")) { update(layer.copy(fontFamily = it)) }
    EditorToggle(stringResource(Res.string.image_editor_bold), layer.fontWeight == EditorFontWeight.BOLD) { update(layer.copy(fontWeight = if (it) EditorFontWeight.BOLD else EditorFontWeight.NORMAL)) }
    EditorToggle(stringResource(Res.string.image_editor_italic), layer.fontStyle == EditorFontStyle.ITALIC) { update(layer.copy(fontStyle = if (it) EditorFontStyle.ITALIC else EditorFontStyle.NORMAL)) }
    EditorToggle(stringResource(Res.string.image_editor_underline), layer.textDecoration == EditorTextDecoration.UNDERLINE) { update(layer.copy(textDecoration = if (it) EditorTextDecoration.UNDERLINE else EditorTextDecoration.NONE)) }
    EditorChoices(stringResource(Res.string.image_editor_align), layer.textAlign, listOf(EditorTextAlign.LEFT to stringResource(Res.string.image_editor_left), EditorTextAlign.CENTER to stringResource(Res.string.image_editor_center), EditorTextAlign.RIGHT to stringResource(Res.string.image_editor_right))) { update(layer.copy(textAlign = it)) }
    EditorNumberField(stringResource(Res.string.image_editor_letter_spacing), layer.letterSpacing, -50.0..100.0) { update(layer.copy(letterSpacing = it)) }
    EditorNumberField(stringResource(Res.string.image_editor_text_width), layer.maxWidth, 1.0..8000.0) { update(layer.copy(maxWidth = it)) }
    EditorNumberField(stringResource(Res.string.image_editor_rotation), layer.rotation, -360.0..360.0) { update(layer.copy(rotation = it)) }
    EditorNumberField(stringResource(Res.string.image_editor_stroke_width), layer.textStroke.width, 0.0..100.0) { update(layer.copy(textStroke = layer.textStroke.copy(width = it))) }
    EditorColorField(stringResource(Res.string.image_editor_stroke_color), layer.textStroke.color) { update(layer.copy(textStroke = layer.textStroke.copy(color = it))) }
    EditorColorField(stringResource(Res.string.image_editor_shadow_color), layer.textShadow.color) { update(layer.copy(textShadow = layer.textShadow.copy(color = it))) }
    EditorNumberField(stringResource(Res.string.image_editor_shadow_x), layer.textShadow.offsetX, -500.0..500.0) { update(layer.copy(textShadow = layer.textShadow.copy(offsetX = it))) }
    EditorNumberField(stringResource(Res.string.image_editor_shadow_y), layer.textShadow.offsetY, -500.0..500.0) { update(layer.copy(textShadow = layer.textShadow.copy(offsetY = it))) }
    EditorNumberField(stringResource(Res.string.image_editor_shadow_blur), layer.textShadow.blur, 0.0..500.0) { update(layer.copy(textShadow = layer.textShadow.copy(blur = it))) }
    EditorToggle(stringResource(Res.string.image_editor_glow), layer.glow) { update(layer.copy(glow = it)) }
}
