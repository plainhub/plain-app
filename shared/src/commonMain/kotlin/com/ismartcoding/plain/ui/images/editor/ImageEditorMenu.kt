package com.ismartcoding.plain.ui.images.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PDropdownMenu
import com.ismartcoding.plain.ui.base.PDropdownMenuItem
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PTextButton
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.ellipsis_vertical
import org.jetbrains.compose.resources.stringResource

@Composable
internal fun ImageEditorMenu(enabled: Boolean, canvasSettings: () -> Unit, preview: () -> Unit, export: (EditorRasterFormat) -> Unit) {
    var open by remember { mutableStateOf(false) }
    Box {
        PIconButton(icon = UiRes.drawable.ellipsis_vertical, contentDescription = stringResource(Res.string.more), enabled = enabled) { open = true }
        PDropdownMenu(open, { open = false }) {
            PDropdownMenuItem({ Text(stringResource(Res.string.image_editor_canvas_settings)) }, { open = false; canvasSettings() })
            PDropdownMenuItem({ Text(stringResource(Res.string.image_editor_preview)) }, { open = false; preview() })
            EditorRasterFormat.entries.forEach { format ->
                PDropdownMenuItem({ Text(stringResource(Res.string.image_editor_export) + " " + format.name) }, { open = false; export(format) })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImageEditorPreview(image: ImageBitmap, dismiss: () -> Unit) {
    PModalBottomSheet(onDismissRequest = dismiss, fullHeight = true) {
        PBottomSheetTopAppBar(title = stringResource(Res.string.image_editor_preview), actions = { PTextButton(stringResource(Res.string.close), dismiss) })
        Image(image, contentDescription = stringResource(Res.string.image_editor_preview), modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}
