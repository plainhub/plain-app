package com.ismartcoding.plain.ui.components.codeeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.copy as ui_drawable_copy
import com.ismartcoding.plain.ui.resources.scissors as ui_drawable_scissors
import com.ismartcoding.plain.ui.resources.select_all as ui_drawable_select_all
import com.ismartcoding.plain.ui.resources.x as ui_drawable_x

@Composable
fun SelectionToolbarOverlay(controller: EditorController, modifier: Modifier = Modifier) {
    val selection = controller.selection.value?.normalized() ?: return
    val clipboard = LocalClipboardManager.current
    val bg = MaterialTheme.colorScheme.surfaceContainerHigh
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.Center,
    ) {
        Row(
            modifier = Modifier
                .clip(androidx.compose.foundation.shape.CircleShape)
                .background(bg.copy(alpha = 0.96f))
                .padding(horizontal = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            EditorIconButton(icon = UiRes.drawable.ui_drawable_copy, tint = tint) {
                controller.selectedText()?.let { clipboard.setText(AnnotatedString(it)) }
            }
            if (!controller.readOnly.value) {
                EditorIconButton(icon = UiRes.drawable.ui_drawable_scissors, tint = tint) {
                    controller.selectedText()?.let { clipboard.setText(AnnotatedString(it)) }
                    controller.deleteSelection()
                }
            }
            EditorIconButton(icon = UiRes.drawable.ui_drawable_select_all, tint = tint) {
                controller.selectAll()
            }
            EditorIconButton(icon = UiRes.drawable.ui_drawable_x, tint = tint) {
                controller.selection.value = null
            }
        }
    }
}
