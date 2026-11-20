@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ismartcoding.plain.ui.images.editor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.yjs.EditorLayer
import com.ismartcoding.plain.lib.yjs.ImageLayer
import com.ismartcoding.plain.lib.yjs.StickerLayer
import com.ismartcoding.plain.lib.yjs.TextLayer
import com.ismartcoding.plain.ui.base.PIconTextSmallButton
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.resources.*
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes

@Composable
internal fun ImageEditorBottomAppBar(
    vm: ImageEditorEditViewModel,
    selectedLayer: EditorLayer?,
    onToolSelected: (ImageEditorTool) -> Unit,
    onGroup: (EditorToolGroup) -> Unit,
    onStyle: () -> Unit,
    onEditText: () -> Unit,
    onReplaceImage: () -> Unit,
    onOrder: () -> Unit,
    cropActions: @Composable () -> Unit,
) {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainer).navigationBarsPadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            PIconButton(UiRes.drawable.undo, contentDescription = stringResource(Res.string.undo), enabled = vm.canUndo.value, click = vm::undo)
            PIconButton(UiRes.drawable.redo, contentDescription = stringResource(Res.string.redo), enabled = vm.canRedo.value, click = vm::redo)
            Spacer(Modifier.weight(1f))
            PTextButton(stringResource(Res.string.image_editor_layers), { vm.showLayerPanel.value = true })
        }
        if (selectedLayer != null) {
            Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(editorLayerTitle(selectedLayer), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                PTextButton(stringResource(Res.string.done), { vm.selectLayer(null); vm.currentTool.value = ImageEditorTool.SELECT })
            }
            EditorActions(buildList {
                when (selectedLayer) {
                    is TextLayer, is StickerLayer -> add(EditorAction(UiRes.drawable.type, stringResource(Res.string.edit), onClick = onEditText))
                    is ImageLayer -> add(EditorAction(UiRes.drawable.image, stringResource(Res.string.image_editor_replace_image), onClick = onReplaceImage))
                    else -> Unit
                }
                add(EditorAction(UiRes.drawable.settings, stringResource(Res.string.style), onClick = onStyle))
                add(EditorAction(UiRes.drawable.copy, stringResource(Res.string.image_editor_duplicate_layer), onClick = { vm.duplicateLayer(selectedLayer.id) }))
                add(EditorAction(UiRes.drawable.layers, stringResource(Res.string.image_editor_layer_order), onClick = onOrder))
                add(EditorAction(UiRes.drawable.trash_2, stringResource(Res.string.delete), onClick = { vm.deleteLayer(selectedLayer.id) }))
            })
        } else {
            val tool = vm.currentTool.value
            if (tool in listOf(ImageEditorTool.FREEHAND, ImageEditorTool.HIGHLIGHT, ImageEditorTool.ARROW, ImageEditorTool.RECT, ImageEditorTool.ELLIPSE, ImageEditorTool.MOSAIC)) {
                Row(Modifier.fillMaxWidth().padding(horizontal = 16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(editorToolLabel(tool)), style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                    PTextButton(stringResource(Res.string.style), onStyle)
                }
            }
            if (tool == ImageEditorTool.CROP) cropActions()
            else {
                EditorActions(listOf(
                    EditorAction(UiRes.drawable.mouse_pointer, stringResource(Res.string.image_editor_tool_select), tool == ImageEditorTool.SELECT) { onToolSelected(ImageEditorTool.SELECT) },
                    EditorAction(UiRes.drawable.pen, stringResource(Res.string.image_editor_tool_freehand), tool in listOf(ImageEditorTool.FREEHAND, ImageEditorTool.HIGHLIGHT)) { onGroup(EditorToolGroup.BRUSH) },
                    EditorAction(UiRes.drawable.square, stringResource(Res.string.image_editor_shapes), tool in listOf(ImageEditorTool.ARROW, ImageEditorTool.RECT, ImageEditorTool.ELLIPSE)) { onGroup(EditorToolGroup.SHAPES) },
                    EditorAction(UiRes.drawable.type, stringResource(Res.string.image_editor_tool_text), tool == ImageEditorTool.TEXT) { onToolSelected(ImageEditorTool.TEXT) },
                    EditorAction(UiRes.drawable.grid_3x3, stringResource(Res.string.image_editor_tool_mosaic), tool == ImageEditorTool.MOSAIC) { onToolSelected(ImageEditorTool.MOSAIC) },
                    EditorAction(UiRes.drawable.more_horiz, stringResource(Res.string.more), tool == ImageEditorTool.STICKER) { onGroup(EditorToolGroup.MORE) },
                ))
            }
        }
    }
}

private data class EditorAction(val icon: DrawableResource, val label: String, val selected: Boolean = false, val onClick: () -> Unit)

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun EditorActions(actions: List<EditorAction>) {
    val measurer = rememberTextMeasurer()
    val density = LocalDensity.current
    val style = MaterialTheme.typography.labelSmall
    val labelWidth = actions.maxOf { action -> action.label.split(' ').maxOf { measurer.measure(AnnotatedString(it), style).size.width } }
    val itemWidth = maxOf(48.dp, with(density) { labelWidth.toDp() } + 8.dp)
    BoxWithConstraints(Modifier.fillMaxWidth().padding(horizontal = 8.dp)) {
        val fitting = (maxWidth / itemWidth).toInt().coerceAtLeast(1)
        val columns = if (fitting >= actions.size) actions.size else minOf(3, fitting)
        FlowRow(maxItemsInEachRow = columns, verticalArrangement = Arrangement.spacedBy(8.dp)) {
            actions.forEach { action ->
                PIconTextSmallButton(
                    icon = action.icon,
                    text = action.label,
                    modifier = Modifier.weight(1f).heightIn(min = 64.dp).clip(MaterialTheme.shapes.small)
                        .background(if (action.selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceContainer)
                        .semantics { selected = action.selected },
                    onClick = action.onClick,
                )
            }
        }
    }
}
