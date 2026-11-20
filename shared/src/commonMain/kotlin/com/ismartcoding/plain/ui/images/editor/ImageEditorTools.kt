@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ismartcoding.plain.ui.images.editor

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.resources.*
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes

internal enum class EditorToolGroup { SHAPES, MORE }

@Composable
internal fun ImageEditorTools(
    group: EditorToolGroup,
    onTool: (ImageEditorTool) -> Unit,
    onAddImage: () -> Unit,
    onCanvas: () -> Unit,
    onPreview: () -> Unit,
    onDismiss: () -> Unit,
) {
    val title = when (group) {
        EditorToolGroup.SHAPES -> Res.string.image_editor_shapes
        EditorToolGroup.MORE -> Res.string.more
    }
    PModalBottomSheet(onDismissRequest = onDismiss) {
        PBottomSheetTopAppBar(title = stringResource(title), navigationIcon = {
            PIconButton(UiRes.drawable.x, contentDescription = stringResource(Res.string.close), click = onDismiss)
        })
        Column(Modifier.padding(bottom = 24.dp)) {
            val actions = when (group) {
                EditorToolGroup.SHAPES -> listOf(
                    EditorAction(UiRes.drawable.arrow_up_right, stringResource(Res.string.image_editor_tool_arrow)) { onTool(ImageEditorTool.ARROW) },
                    EditorAction(UiRes.drawable.square, stringResource(Res.string.image_editor_tool_rect)) { onTool(ImageEditorTool.RECT) },
                    EditorAction(UiRes.drawable.circle, stringResource(Res.string.image_editor_tool_ellipse)) { onTool(ImageEditorTool.ELLIPSE) },
                )
                EditorToolGroup.MORE -> listOf(
                    EditorAction(UiRes.drawable.image, stringResource(Res.string.image_editor_add_image), onClick = onAddImage),
                    EditorAction(UiRes.drawable.sticky_note, stringResource(Res.string.image_editor_tool_sticker)) { onTool(ImageEditorTool.STICKER) },
                    EditorAction(UiRes.drawable.scissors, stringResource(Res.string.image_editor_crop)) { onTool(ImageEditorTool.CROP) },
                    EditorAction(UiRes.drawable.settings, stringResource(Res.string.image_editor_canvas_settings), onClick = onCanvas),
                    EditorAction(UiRes.drawable.eye, stringResource(Res.string.image_editor_preview), onClick = onPreview),
                )
            }
            EditorActions(actions, maxColumns = 3)
        }
    }
}

internal fun editorToolLabel(tool: ImageEditorTool): StringResource = when (tool) {
    ImageEditorTool.SELECT -> Res.string.image_editor_tool_select
    ImageEditorTool.CROP -> Res.string.image_editor_crop
    ImageEditorTool.FREEHAND -> Res.string.image_editor_tool_freehand
    ImageEditorTool.ARROW -> Res.string.image_editor_tool_arrow
    ImageEditorTool.RECT -> Res.string.image_editor_tool_rect
    ImageEditorTool.ELLIPSE -> Res.string.image_editor_tool_ellipse
    ImageEditorTool.HIGHLIGHT -> Res.string.image_editor_tool_highlight
    ImageEditorTool.MOSAIC -> Res.string.image_editor_tool_mosaic
    ImageEditorTool.TEXT -> Res.string.image_editor_tool_text
    ImageEditorTool.STICKER -> Res.string.image_editor_tool_sticker
}
