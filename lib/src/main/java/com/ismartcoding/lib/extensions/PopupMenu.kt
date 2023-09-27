package com.ismartcoding.lib.extensions

import android.view.MenuItem
import androidx.appcompat.widget.PopupMenu

fun PopupMenu.onItemClick(block: MenuItem.() -> Unit) {
    setOnMenuItemClickListener {
        block(it)
        true
    }
}
