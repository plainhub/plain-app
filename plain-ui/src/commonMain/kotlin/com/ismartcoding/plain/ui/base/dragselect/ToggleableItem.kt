package com.ismartcoding.plain.ui.base.dragselect

import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.ui.Modifier

fun Modifier.dragSelectToggleableItem(
    state: DragSelectState,
    id: String,
    semanticsLabel: String = DEFAULT_LABEL,
    interactionSource: MutableInteractionSource? = null,
): Modifier = then(
    Modifier
        .dragSelectSemantics(state, id, semanticsLabel)
        .dragSelectToggleable(state, id, interactionSource)
)