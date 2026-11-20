@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ismartcoding.plain.ui.images.editor

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.yjs.EditorLayer
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.resources.*
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes

@Composable
internal fun ImageEditorLayerOrder(layer: EditorLayer, vm: ImageEditorEditViewModel, dismiss: () -> Unit) {
    PModalBottomSheet(onDismissRequest = dismiss) {
        PBottomSheetTopAppBar(title = stringResource(Res.string.image_editor_layer_order), subtitle = editorLayerTitle(layer), navigationIcon = {
            PIconButton(UiRes.drawable.x, contentDescription = stringResource(Res.string.close), click = dismiss)
        })
        Column(Modifier.padding(bottom = 24.dp)) {
            val index = vm.layers.indexOfFirst { it.id == layer.id }
            PSheetActionRow(UiRes.drawable.chevron_up, stringResource(Res.string.image_editor_bring_forward), enabled = index < vm.layers.lastIndex) { vm.bringForward(layer.id) }
            PSheetActionRow(UiRes.drawable.expand_more, stringResource(Res.string.image_editor_send_backward), enabled = index > 0) { vm.sendBackward(layer.id) }
            PSheetActionRow(if (layer.visible) UiRes.drawable.eye else UiRes.drawable.eye_off, stringResource(Res.string.image_editor_layer_visibility)) { vm.toggleVisibility(layer.id) }
        }
    }
}
