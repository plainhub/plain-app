package com.ismartcoding.plain.ui.base.dragselect

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.selection.toggleable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed

fun Modifier.dragSelectToggleable(
    state: DragSelectState,
    id: String,
    interactionSource: MutableInteractionSource? = null,
): Modifier = dragSelectToggleable(
    inSelectionMode = state.selectMode,
    selected = state.selectedIds.contains(id),
    interactionSource = interactionSource,
) { _ ->
    state.select(id)
}

fun Modifier.dragSelectToggleable(
    inSelectionMode: Boolean,
    selected: Boolean,
    interactionSource: MutableInteractionSource? = null,
    onToggle: (toggled: Boolean) -> Unit,
): Modifier = composed {
    val interaction = interactionSource ?: remember { MutableInteractionSource() }
    if (!inSelectionMode) {
        Modifier
    } else {
        then(
            Modifier.toggleable(
                value = selected,
                interactionSource = interaction,
                indication = null,
                onValueChange = onToggle
            ),
        )
    }
}