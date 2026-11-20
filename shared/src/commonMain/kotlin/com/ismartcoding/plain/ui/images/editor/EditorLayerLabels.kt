package com.ismartcoding.plain.ui.images.editor

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.yjs.*
import com.ismartcoding.plain.ui.resources.*
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes

@Composable
internal fun editorLayerTitle(layer: EditorLayer): String = stringResource(when (layer) {
    is ArrowLayer -> Res.string.image_editor_tool_arrow
    is RectLayer -> Res.string.image_editor_tool_rect
    is EllipseLayer -> Res.string.image_editor_tool_ellipse
    is HighlightLayer -> Res.string.image_editor_tool_highlight
    is MosaicLayer -> Res.string.image_editor_tool_mosaic
    is TextLayer -> Res.string.image_editor_tool_text
    is FreehandLayer -> Res.string.image_editor_tool_freehand
    is StickerLayer -> Res.string.image_editor_tool_sticker
    is ImageLayer -> Res.string.image_editor_tool_image
})

internal fun editorLayerIcon(layer: EditorLayer) = when (layer) {
    is ArrowLayer -> UiRes.drawable.arrow_up_right
    is RectLayer -> UiRes.drawable.square
    is EllipseLayer -> UiRes.drawable.circle
    is HighlightLayer -> UiRes.drawable.highlighter
    is MosaicLayer -> UiRes.drawable.grid_3x3
    is TextLayer -> UiRes.drawable.type
    is FreehandLayer -> UiRes.drawable.pen
    is StickerLayer -> UiRes.drawable.sticky_note
    is ImageLayer -> UiRes.drawable.image
}
