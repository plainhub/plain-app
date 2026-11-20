package com.ismartcoding.plain.ui.images.editor

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.yjs.*
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.resources.x
import com.ismartcoding.plain.ui.resources.Res as UiRes
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImageEditorProperties(layer: EditorLayer, update: (EditorLayer) -> Unit, dismiss: () -> Unit) {
    PModalBottomSheet(onDismissRequest = dismiss) {
        PBottomSheetTopAppBar(title = stringResource(Res.string.style), subtitle = editorLayerTitle(layer), navigationIcon = { PIconButton(UiRes.drawable.x, contentDescription = stringResource(Res.string.close), click = dismiss) })
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            val color = when (layer) {
                is ArrowLayer -> layer.color
                is RectLayer -> layer.color
                is EllipseLayer -> layer.color
                is HighlightLayer -> layer.color
                is TextLayer -> layer.color
                is StickerLayer -> layer.color
                is FreehandLayer -> layer.color
                else -> null
            }
            color?.let { EditorColorField(stringResource(Res.string.image_editor_color), it) { value ->
                update(when (layer) {
                    is ArrowLayer -> layer.copy(color = value)
                    is RectLayer -> layer.copy(color = value)
                    is EllipseLayer -> layer.copy(color = value)
                    is HighlightLayer -> layer.copy(color = value)
                    is TextLayer -> layer.copy(color = value)
                    is StickerLayer -> layer.copy(color = value)
                    is FreehandLayer -> layer.copy(color = value)
                    else -> layer
                })
            } }
            val width = when (layer) {
                is ArrowLayer -> layer.lineWidth
                is RectLayer -> layer.lineWidth
                is EllipseLayer -> layer.lineWidth
                is FreehandLayer -> layer.lineWidth
                else -> null
            }
            width?.let { EditorNumberField(stringResource(Res.string.image_editor_line_width), it, 1.0..100.0) { value ->
                update(when (layer) {
                    is ArrowLayer -> layer.copy(lineWidth = value)
                    is RectLayer -> layer.copy(lineWidth = value)
                    is EllipseLayer -> layer.copy(lineWidth = value)
                    is FreehandLayer -> layer.copy(lineWidth = value)
                    else -> layer
                })
            } }
            when (layer) {
                is TextLayer -> {
                    EditorTextProperties(layer) { update(it) }
                }
                is StickerLayer -> {
                    EditorNumberField(stringResource(Res.string.image_editor_font_size), layer.fontSize, 1.0..500.0) { update(layer.copy(fontSize = it)) }
                    EditorToggle(stringResource(Res.string.image_editor_bold), layer.fontWeight == EditorFontWeight.BOLD) { update(layer.copy(fontWeight = if (it) EditorFontWeight.BOLD else EditorFontWeight.NORMAL)) }
                    EditorToggle(stringResource(Res.string.image_editor_italic), layer.fontStyle == EditorFontStyle.ITALIC) { update(layer.copy(fontStyle = if (it) EditorFontStyle.ITALIC else EditorFontStyle.NORMAL)) }
                    EditorNumberField(stringResource(Res.string.image_editor_rotation), layer.rotation, -360.0..360.0) { update(layer.copy(rotation = it)) }
                }
                is ImageLayer -> {
                    EditorNumberField(stringResource(Res.string.image_editor_opacity), layer.opacity * 100, 0.0..100.0) { update(layer.copy(opacity = it / 100)) }
                    EditorNumberField(stringResource(Res.string.image_editor_rotation), layer.rotation, -360.0..360.0) { update(layer.copy(rotation = it)) }
                }
                is MosaicLayer -> EditorNumberField(stringResource(Res.string.image_editor_block_size), layer.blockSize, 1.0..100.0) { update(layer.copy(blockSize = it)) }
                is ArrowLayer -> EditorNumberField(stringResource(Res.string.image_editor_rotation), layer.rotation, -360.0..360.0) { update(layer.copy(rotation = it)) }
                else -> Unit
            }
        }
    }
}
