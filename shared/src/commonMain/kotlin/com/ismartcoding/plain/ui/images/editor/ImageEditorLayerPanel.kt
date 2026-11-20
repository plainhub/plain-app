@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ismartcoding.plain.ui.images.editor

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.yjs.*
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.resources.*
import org.jetbrains.compose.resources.painterResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes

@Composable
fun ImageEditorLayerPanel(vm: ImageEditorEditViewModel, onClear: () -> Unit, onDismiss: () -> Unit) {
    val document by vm.document.collectAsState()
    PModalBottomSheet(onDismissRequest = onDismiss) {
        PBottomSheetTopAppBar(title = stringResource(Res.string.image_editor_layers), navigationIcon = {
            PIconButton(UiRes.drawable.x, contentDescription = stringResource(Res.string.close), click = onDismiss)
        })
        LazyColumn(Modifier.fillMaxWidth().padding(bottom = 24.dp)) {
            if (document.layers.isEmpty()) item { Text(stringResource(Res.string.image_editor_no_layers), modifier = Modifier.padding(32.dp), style = MaterialTheme.typography.bodyLarge) }
            items(document.layers.reversed(), key = { it.id }) { layer ->
                val type = editorLayerTitle(layer)
                val content = when (layer) { is TextLayer -> layer.text; is StickerLayer -> layer.text; else -> "" }
                PListItem(
                    title = if (layer is FreehandLayer) "$type (${layer.points.size} pts)" else content.ifEmpty { type },
                    subtitle = if (content.isNotEmpty()) type else "",
                    dimmed = !layer.visible,
                    start = { Icon(painterResource(editorLayerIcon(layer)), contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
                    action = {
                        PIconButton(if (layer.visible) UiRes.drawable.eye else UiRes.drawable.eye_off,
                            contentDescription = stringResource(Res.string.image_editor_layer_visibility)) { vm.toggleVisibility(layer.id) }
                    },
                    modifier = Modifier.clickable { vm.currentTool.value = ImageEditorTool.SELECT; vm.selectLayer(layer.id); onDismiss() },
                )
            }
            if (document.layers.isNotEmpty()) item {
                PTextButton(stringResource(Res.string.image_editor_clear_all), onClear,
                    modifier = Modifier.padding(horizontal = 16.dp), contentColor = MaterialTheme.colorScheme.error)
            }
        }
    }
}
