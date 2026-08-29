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
                        icon = Res.drawable.x,
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
            icon = if (layer.visible) Res.drawable.eye else Res.drawable.eye_off,
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
            icon = Res.drawable.chevron_up,
            contentDescription = stringResource(Res.string.image_editor_bring_forward),
            iconSize = 18.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            click = onBringForward,
        )
        PIconButton(
            icon = Res.drawable.expand_more,
            contentDescription = stringResource(Res.string.image_editor_send_backward),
            iconSize = 18.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            click = onSendBackward,
        )
        PIconButton(
            icon = Res.drawable.copy,
            contentDescription = stringResource(Res.string.image_editor_duplicate_layer),
            iconSize = 18.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            click = onDuplicate,
        )
        PIconButton(
            icon = Res.drawable.trash_2,
            contentDescription = stringResource(Res.string.image_editor_delete_layer),
            iconSize = 18.dp,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            click = onDelete,
        )
    }
}

private fun iconForLayer(layer: EditorLayer) = when (layer) {
    is ArrowLayer -> Res.drawable.arrow_up_right
    is RectLayer -> Res.drawable.square
    is EllipseLayer -> Res.drawable.circle
    is HighlightLayer -> Res.drawable.highlighter
    is MosaicLayer -> Res.drawable.grid_3x3
    is TextLayer -> Res.drawable.type
    is FreehandLayer -> Res.drawable.pen
    is StickerLayer -> Res.drawable.sticky_note
    is ImageLayer -> Res.drawable.image
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
