package com.ismartcoding.plain.ui.components.codeeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.extensions.formatBytes
import org.jetbrains.compose.resources.stringResource

/** Single-line status strip: caret position (edit mode), encoding, file size, dirty flag. */
@Composable
fun EditorStatusBar(controller: EditorController) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surfaceContainerLowest)
            .padding(horizontal = 12.dp, vertical = 4.dp),
    ) {
        val parts = buildList {
            if (!controller.readOnly.value) {
                add("Ln ${controller.activeLine.value + 1}, Col ${controller.activeCol.value + 1}")
            }
            add(controller.encodingLabel.value)
            add(controller.openFileSize.formatBytes())
            if (controller.isDirty.value) {
                add(stringResource(Res.string.modified))
            }
        }
        Text(
            text = parts.joinToString("   "),
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            color = color,
            maxLines = 1,
        )
    }
}
