package com.ismartcoding.plain.ui.images.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.x
import com.ismartcoding.plain.ui.resources.copy
import com.ismartcoding.plain.ui.base.PSheetActionRow
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImageEditorExportSheet(copy: () -> Unit, export: (EditorRasterFormat) -> Unit, dismiss: () -> Unit) {
    PModalBottomSheet(onDismissRequest = dismiss) {
        PBottomSheetTopAppBar(title = stringResource(Res.string.image_editor_export), navigationIcon = {
            PIconButton(UiRes.drawable.x, contentDescription = stringResource(Res.string.close), click = dismiss)
        })
        Column(Modifier.padding(bottom = 24.dp)) {
            EditorRasterFormat.entries.forEach { format ->
                PSheetActionRow(title = format.name) { dismiss(); export(format) }
            }
            PSheetActionRow(UiRes.drawable.copy, stringResource(Res.string.image_editor_copy_image)) { dismiss(); copy() }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImageEditorPreview(image: ImageBitmap, dismiss: () -> Unit) {
    PModalBottomSheet(onDismissRequest = dismiss, fullHeight = true) {
        PBottomSheetTopAppBar(title = stringResource(Res.string.image_editor_preview), navigationIcon = { PIconButton(UiRes.drawable.x, contentDescription = stringResource(Res.string.close), click = dismiss) })
        Image(image, contentDescription = stringResource(Res.string.image_editor_preview), modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Fit)
    }
}
