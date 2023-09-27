package com.ismartcoding.plain.ui.extensions

import android.view.MenuItem
import androidx.annotation.MenuRes
import androidx.core.content.ContextCompat
import com.google.android.material.bottomappbar.BottomAppBar
import com.ismartcoding.plain.R

fun BottomAppBar.initMenu(
    @MenuRes menuId: Int,
    overflow: Boolean = false,
) {
    menu.clear()
    inflateMenu(menuId)
    if (overflow) {
        overflowIcon = ContextCompat.getDrawable(this.context, R.drawable.ic_more_vert)
    }
}

fun BottomAppBar.onMenuItemClick(callback: MenuItem.() -> Unit) {
    setOnMenuItemClickListener { menuItem ->
        callback(menuItem)
        true
    }
}
