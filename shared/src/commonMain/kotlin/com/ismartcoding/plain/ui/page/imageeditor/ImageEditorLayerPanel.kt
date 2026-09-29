package com.ismartcoding.plain.ui.page.imageeditor

import com.ismartcoding.plain.i18n.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.base.PIconButton
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.models.ImageEditorEditViewModel
import com.ismartcoding.plain.lib.yjs.ArrowLayer
import com.ismartcoding.plain.lib.yjs.EditorLayer
import com.ismartcoding.plain.lib.yjs.EllipseLayer
import com.ismartcoding.plain.lib.yjs.FreehandLayer
import com.ismartcoding.plain.lib.yjs.HighlightLayer
import com.ismartcoding.plain.lib.yjs.ImageLayer
import com.ismartcoding.plain.lib.yjs.MosaicLayer
import com.ismartcoding.plain.lib.yjs.RectLayer
import com.ismartcoding.plain.lib.yjs.StickerLayer
import com.ismartcoding.plain.lib.yjs.TextLayer
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.arrow_up_right as ui_drawable_arrow_up_right
import com.ismartcoding.plain.ui.resources.chevron_up as ui_drawable_chevron_up
import com.ismartcoding.plain.ui.resources.circle as ui_drawable_circle
import com.ismartcoding.plain.ui.resources.copy as ui_drawable_copy
import com.ismartcoding.plain.ui.resources.expand_more as ui_drawable_expand_more
import com.ismartcoding.plain.ui.resources.eye as ui_drawable_eye
import com.ismartcoding.plain.ui.resources.eye_off as ui_drawable_eye_off
import com.ismartcoding.plain.ui.resources.grid_3x3 as ui_drawable_grid_3x3
import com.ismartcoding.plain.ui.resources.highlighter as ui_drawable_highlighter
import com.ismartcoding.plain.ui.resources.image as ui_drawable_image
import com.ismartcoding.plain.ui.resources.pen as ui_drawable_pen
import com.ismartcoding.plain.ui.resources.square as ui_drawable_square
import com.ismartcoding.plain.ui.resources.sticky_note as ui_drawable_sticky_note
import com.ismartcoding.plain.ui.resources.trash_2 as ui_drawable_trash_2
import com.ismartcoding.plain.ui.resources.type as ui_drawable_type
import com.ismartcoding.plain.ui.resources.x as ui_drawable_x
import com.ismartcoding.plain.i18n.copy
import com.ismartcoding.plain.i18n.image
import com.ismartcoding.plain.i18n.type

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ImageEditorLayerPanel(
    vm: ImageEditorEditViewModel,
    onDismiss: () -> Unit,
) {
    val layers by vm.layersFlow.collectAsState()
    PModalBottomSheet(onDismissRequest = onDismiss) {
        Column {
            PBottomSheetTopAppBar(
                title = stringResource(Res.string.image_editor_layers),
                actions = {
                    PIconButton(
                        icon = UiRes.drawable.ui_drawable_x,
                        contentDescription = stringResource(Res.string.close),
                        click = onDismiss,
                    )
                },
            )
            if (layers.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        text = stringResource(Res.string.image_editor_no_layers),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                ) {
                    items(layers.reversed(), key = { it.id }) { layer ->
                        LayerRow(
                            layer = layer,
                            selected = vm.selectedLayerId.value == layer.id,
                            onClick = { vm.selectLayer(layer.id) },
                            onToggleVisibility = { vm.toggleVisibility(layer.id) },
                            onDuplicate = { vm.duplicateLayer(layer.id) },
                            onBringForward = { vm.bringForward(layer.id) },
                            onSendBackward = { vm.sendBackward(layer.id) },
                            onDelete = { vm.deleteLayer(layer.id) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun LayerRow(
    layer: EditorLayer,
    selected: Boolean,
    onClick: () -> Unit,
    onToggleVisibility: () -> Unit,
    onDuplicate: () -> Unit,
    onBringForward: () -> Unit,
    onSendBackward: () -> Unit,
    onDelete: () -> Unit,
) {
    val bg = if (selected) {
        MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
    } else {
        Color.Transparent
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bg)
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        PIconButton(
            icon = if (layer.visible) UiRes.drawable.ui_drawable_eye else UiRes.drawable.ui_drawable_eye_off,
            contentDescription = stringResource(Res.string.image_editor_layer_visibility),
            iconSize = 20.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            click = onToggleVisibility,
        )
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(iconForLayer(layer)),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = labelForLayer(layer),
            style = MaterialTheme.typography.bodyMedium,
            color = if (layer.visible) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        PIconButton(
            icon = UiRes.drawable.ui_drawable_chevron_up,
            contentDescription = stringResource(Res.string.image_editor_bring_forward),
            iconSize = 18.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            click = onBringForward,
        )
        PIconButton(
            icon = UiRes.drawable.ui_drawable_expand_more,
            contentDescription = stringResource(Res.string.image_editor_send_backward),
            iconSize = 18.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            click = onSendBackward,
        )
        PIconButton(
            icon = UiRes.drawable.ui_drawable_copy,
            contentDescription = stringResource(Res.string.image_editor_duplicate_layer),
            iconSize = 18.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            click = onDuplicate,
        )
        PIconButton(
            icon = UiRes.drawable.ui_drawable_trash_2,
            contentDescription = stringResource(Res.string.image_editor_delete_layer),
            iconSize = 18.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            click = onDelete,
        )
    }
}

private fun iconForLayer(layer: EditorLayer) = when (layer) {
    is ArrowLayer -> UiRes.drawable.ui_drawable_arrow_up_right
    is RectLayer -> UiRes.drawable.ui_drawable_square
    is EllipseLayer -> UiRes.drawable.ui_drawable_circle
    is HighlightLayer -> UiRes.drawable.ui_drawable_highlighter
    is MosaicLayer -> UiRes.drawable.ui_drawable_grid_3x3
    is TextLayer -> UiRes.drawable.ui_drawable_type
    is FreehandLayer -> UiRes.drawable.ui_drawable_pen
    is StickerLayer -> UiRes.drawable.ui_drawable_sticky_note
    is ImageLayer -> UiRes.drawable.ui_drawable_image
}

private fun labelForLayer(layer: EditorLayer) = when (layer) {
    is ArrowLayer -> "Arrow"
    is RectLayer -> "Rectangle"
    is EllipseLayer -> "Ellipse"
    is HighlightLayer -> "Highlight"
    is MosaicLayer -> "Mosaic"
    is TextLayer -> "Text: ${layer.text.take(20)}"
    is FreehandLayer -> "Brush (${layer.points.size} pts)"
    is StickerLayer -> "Sticker: ${layer.text.take(20)}"
    is ImageLayer -> "Image"
}
