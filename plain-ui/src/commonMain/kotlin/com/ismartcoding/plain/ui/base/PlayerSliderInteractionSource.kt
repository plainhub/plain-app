package com.ismartcoding.plain.ui.base

import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.Interaction
import androidx.compose.foundation.interaction.MutableInteractionSource

// Draggable calls its finish callback after both Stop and Cancel. Reset before
// forwarding Cancel so that the slider cannot commit a cancelled seek.
internal class PlayerSliderInteractionSource(private val onCancel: () -> Unit) : MutableInteractionSource {
    private val delegate = MutableInteractionSource()
    override val interactions get() = delegate.interactions

    override suspend fun emit(interaction: Interaction) {
        if (interaction is DragInteraction.Cancel) onCancel()
        delegate.emit(interaction)
    }

    override fun tryEmit(interaction: Interaction): Boolean {
        if (interaction is DragInteraction.Cancel) onCancel()
        return delegate.tryEmit(interaction)
    }
}
