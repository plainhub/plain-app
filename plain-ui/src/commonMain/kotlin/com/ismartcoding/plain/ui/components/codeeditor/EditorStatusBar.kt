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
            add(formatEditorBytes(controller.openFileSize))
            if (controller.isDirty.value) {
                add("Modified")
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

private fun formatEditorBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024L * 1024 -> "${bytes / 1024} KB"
    bytes < 1024L * 1024 * 1024 -> "${bytes / (1024 * 1024)} MB"
    else -> "${bytes / (1024L * 1024 * 1024)} GB"
}
