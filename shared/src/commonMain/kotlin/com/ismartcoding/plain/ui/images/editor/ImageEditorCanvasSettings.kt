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
import com.ismartcoding.plain.lib.yjs.Point
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PTextButton
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImageEditorCanvasSettings(document: EditorDocumentState, vm: ImageEditorEditViewModel, pickBackground: () -> Unit, dismiss: () -> Unit) {
    PModalBottomSheet(onDismissRequest = dismiss, fullHeight = true) {
        PBottomSheetTopAppBar(title = stringResource(Res.string.image_editor_canvas_settings), actions = { PTextButton(stringResource(Res.string.close), dismiss) })
        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            EditorNumberField(stringResource(Res.string.image_editor_width), document.canvasSize.width.toDouble(), 1.0..8000.0) { vm.setCanvasSize(it.toInt(), document.canvasSize.height) }
            EditorNumberField(stringResource(Res.string.image_editor_height), document.canvasSize.height.toDouble(), 1.0..8000.0) { vm.setCanvasSize(document.canvasSize.width, it.toInt()) }
            PTextButton("HD 1920 × 1080", { vm.setCanvasSize(1920, 1080) })
            PTextButton("4K 3840 × 2160", { vm.setCanvasSize(3840, 2160) })
            EditorChoices(stringResource(Res.string.image_editor_background), document.bgColor, listOf("transparent" to stringResource(Res.string.image_editor_transparent), "#ffffff" to stringResource(Res.string.image_editor_white), "#000000" to stringResource(Res.string.image_editor_black), "#f3f4f6" to stringResource(Res.string.image_editor_gray), "#fef3c7" to stringResource(Res.string.image_editor_yellow), "#dbeafe" to stringResource(Res.string.image_editor_blue), "#dcfce7" to stringResource(Res.string.image_editor_green), "#fce7f3" to stringResource(Res.string.image_editor_pink)), vm::setBgColor)
            EditorColorField(stringResource(Res.string.image_editor_custom_color), document.bgColor, vm::setBgColor)
            PTextButton(stringResource(Res.string.image_editor_background_image), pickBackground)
            if (document.sourceImage != null) {
                PTextButton(stringResource(Res.string.image_editor_remove_background), { vm.setBackgroundImage(null) })
                EditorNumberField(stringResource(Res.string.image_editor_opacity), document.imgAlpha, 0.0..100.0, vm::setBackgroundAlpha)
                EditorNumberField(stringResource(Res.string.image_editor_image_offset_x), document.imgOffset.x, -8000.0..8000.0) { vm.setBackgroundOffset(document.imgOffset.copy(x = it)) }
                EditorNumberField(stringResource(Res.string.image_editor_image_offset_y), document.imgOffset.y, -8000.0..8000.0) { vm.setBackgroundOffset(document.imgOffset.copy(y = it)) }
                PTextButton(stringResource(Res.string.image_editor_reset_offset), { vm.setBackgroundOffset(Point(0.0, 0.0)) })
            }
        }
    }
}
