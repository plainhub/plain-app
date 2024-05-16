package com.ismartcoding.plain.ui.base.dragselect

import androidx.compose.runtime.Stable
import androidx.compose.runtime.saveable.Saver

@Stable
data class DragState(
    val initialId: String = "",
    val initial: Int = NONE,
    val current: Int = NONE,
) {
    val isDragging: Boolean
        get() = initial != NONE && current != NONE

    companion object {

        const val NONE = -1

        val Saver = Saver<DragState, Pair<Int, Int>>(
            save = { it.initial to it.current },
            restore = { (initial, current) -> DragState(initial = initial, current = current) },
        )
    }
}