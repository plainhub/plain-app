@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.ismartcoding.plain.ui.images.editor

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.*
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.resources.x
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.ui.resources.Res as UiRes

@Composable
internal fun ImageEditorToolStyle(vm: ImageEditorEditViewModel, dismiss: () -> Unit) {
    PModalBottomSheet(onDismissRequest = dismiss) {
        PBottomSheetTopAppBar(title = stringResource(editorToolLabel(vm.currentTool.value)), navigationIcon = {
            PIconButton(UiRes.drawable.x, contentDescription = stringResource(Res.string.close), click = dismiss)
        })
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (vm.currentTool.value == ImageEditorTool.MOSAIC) {
                EditorNumberField(stringResource(Res.string.image_editor_block_size), vm.mosaicBlockSize.value, 1.0..100.0) { vm.mosaicBlockSize.value = it }
            } else {
                if (vm.currentTool.value == ImageEditorTool.FREEHAND || vm.currentTool.value == ImageEditorTool.HIGHLIGHT) {
                    EditorChoices(stringResource(Res.string.image_editor_tool_freehand), vm.currentTool.value,
                        listOf(ImageEditorTool.FREEHAND, ImageEditorTool.HIGHLIGHT).map { it to stringResource(editorToolLabel(it)) }) {
                        vm.currentTool.value = it
                    }
                }
                EditorChoices(stringResource(Res.string.image_editor_color), vm.color.value,
                    listOf("#ff0000", "#00cc00", "#0066ff", "#ffcc00", "#ff8800", "#ff00ff", "#00cccc", "#000000").map { it to it }) { vm.color.value = it }
                EditorColorField(stringResource(Res.string.image_editor_custom_color), vm.color.value) { vm.color.value = it }
                if (vm.currentTool.value != ImageEditorTool.HIGHLIGHT) {
                    EditorChoices(stringResource(Res.string.image_editor_line_width), vm.lineWidth.value, listOf(2.0, 4.0, 8.0, 16.0).map { it to it.toInt().toString() }) { vm.lineWidth.value = it }
                }
            }
        }
    }
}
