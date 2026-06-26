package com.ismartcoding.plain.ui.base.dragselect

import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics

internal const val DEFAULT_LABEL = "Select"

fun Modifier.dragSelectSemantics(
    dragSelectState: DragSelectState,
    id: String,
    label: String = DEFAULT_LABEL,
): Modifier = dragSelectSemantics(dragSelectState, label) {
    dragSelectState.addSelected(id)
}

fun Modifier.dragSelectSemantics(
    dragSelectState: DragSelectState,
    label: String = DEFAULT_LABEL,
    onLongClick: () -> Unit,
): Modifier = dragSelectSemantics(dragSelectState.selectMode, label, onLongClick)

fun Modifier.dragSelectSemantics(
    inSelectionMode: Boolean,
    label: String = DEFAULT_LABEL,
    onLongClick: () -> Unit,
): Modifier = then(
    Modifier.semantics {
        if (!inSelectionMode) {
            onLongClick(label = label) {
                onLongClick()
                true
            }
        }
    },
)
