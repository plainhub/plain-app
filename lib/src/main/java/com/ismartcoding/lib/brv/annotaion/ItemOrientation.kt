package com.ismartcoding.lib.brv.annotaion

import androidx.recyclerview.widget.ItemTouchHelper

object ItemOrientation {
    const val ALL =
        ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT or ItemTouchHelper.UP or ItemTouchHelper.DOWN
    const val VERTICAL = ItemTouchHelper.UP or ItemTouchHelper.DOWN
    const val HORIZONTAL = ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
    const val LEFT = ItemTouchHelper.LEFT
    const val RIGHT = ItemTouchHelper.RIGHT
    const val UP = ItemTouchHelper.UP
    const val DOWN = ItemTouchHelper.DOWN
    const val NONE = 0
}
