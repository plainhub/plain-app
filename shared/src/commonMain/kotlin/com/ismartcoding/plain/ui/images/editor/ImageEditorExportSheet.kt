package com.ismartcoding.plain.ui.images.editor

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.ui.base.PBottomSheetTopAppBar
import com.ismartcoding.plain.ui.base.PFilledButton
import com.ismartcoding.plain.ui.base.PModalBottomSheet
import com.ismartcoding.plain.ui.base.PSegmentedButtons
import com.ismartcoding.plain.ui.base.PSheetActionRow
import com.ismartcoding.plain.ui.components.PIconButton
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.copy
import com.ismartcoding.plain.ui.resources.eye
import com.ismartcoding.plain.ui.resources.x
import org.jetbrains.compose.resources.stringResource

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImageEditorExportSheet(preview: () -> Unit, copy: () -> Unit, export: (EditorRasterFormat) -> Unit, dismiss: () -> Unit) {
    val formats = EditorRasterFormat.entries
    var selected by remember { mutableStateOf(EditorRasterFormat.PNG) }
    PModalBottomSheet(onDismissRequest = dismiss) {
        PBottomSheetTopAppBar(title = stringResource(Res.string.image_editor_export), navigationIcon = {
            PIconButton(UiRes.drawable.x, contentDescription = stringResource(Res.string.close), click = dismiss)
        })
        Column(Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            PSegmentedButtons(formats, selected.ordinal, { selected = formats[it] }, { it.name }, modifier = Modifier.fillMaxWidth())
            PSheetActionRow(UiRes.drawable.eye, stringResource(Res.string.image_editor_preview)) { dismiss(); preview() }
            PSheetActionRow(UiRes.drawable.copy, stringResource(Res.string.image_editor_copy_image)) { dismiss(); copy() }
            PFilledButton(stringResource(Res.string.image_editor_export), { dismiss(); export(selected) }, modifier = Modifier.align(Alignment.End))
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
