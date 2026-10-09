package com.ismartcoding.plain.ui.components.codeeditor

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.chevron_down as ui_drawable_chevron_down
import com.ismartcoding.plain.ui.resources.chevron_left as ui_drawable_chevron_left
import com.ismartcoding.plain.ui.resources.chevron_right as ui_drawable_chevron_right
import com.ismartcoding.plain.ui.resources.chevron_up as ui_drawable_chevron_up
import com.ismartcoding.plain.ui.resources.delete_forever as ui_drawable_delete_forever
import com.ismartcoding.plain.ui.resources.redo_2 as ui_drawable_redo_2
import com.ismartcoding.plain.ui.resources.undo_2 as ui_drawable_undo_2

@Composable
fun EditAssistToolbar(controller: EditorController, modifier: Modifier = Modifier) {
    if (controller.readOnly.value) return
    val bg = MaterialTheme.colorScheme.surfaceContainerHigh
    val tint = MaterialTheme.colorScheme.onSurfaceVariant
    Row(
        modifier = modifier
            .padding(8.dp)
            .clip(androidx.compose.foundation.shape.CircleShape)
            .background(bg.copy(alpha = 0.94f))
            .padding(horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        EditorIconButton(icon = UiRes.drawable.ui_drawable_chevron_up, tint = tint) { controller.moveCaretRelative(-1, 0) }
        EditorIconButton(icon = UiRes.drawable.ui_drawable_chevron_down, tint = tint) { controller.moveCaretRelative(1, 0) }
        EditorIconButton(icon = UiRes.drawable.ui_drawable_chevron_left, tint = tint) { controller.moveCaretRelative(0, -1) }
        EditorIconButton(icon = UiRes.drawable.ui_drawable_chevron_right, tint = tint) { controller.moveCaretRelative(0, 1) }
        EditorIconButton(icon = UiRes.drawable.ui_drawable_delete_forever, tint = tint) {
            if (controller.selection.value != null) {
                controller.deleteSelection()
            } else if (controller.activeCol.value == 0) {
                controller.joinWithPreviousLine()
            }
        }
        EditorIconButton(
            icon = UiRes.drawable.ui_drawable_undo_2,
            enabled = controller.canUndo.value,
            tint = if (controller.canUndo.value) MaterialTheme.colorScheme.onSurface else tint.copy(alpha = 0.4f),
        ) { controller.undo() }
        EditorIconButton(
            icon = UiRes.drawable.ui_drawable_redo_2,
            enabled = controller.canRedo.value,
            tint = if (controller.canRedo.value) MaterialTheme.colorScheme.onSurface else tint.copy(alpha = 0.4f),
        ) { controller.redo() }
    }
}
