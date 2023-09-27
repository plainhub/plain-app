package com.ismartcoding.lib.extensions

import android.graphics.Color
import android.view.*
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

fun Window.fullScreen() {
    addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
    statusBarColor = Color.TRANSPARENT
    decorView.setPadding(0, 0, 0, 0)
    val lp = attributes
    lp.width = WindowManager.LayoutParams.MATCH_PARENT
    lp.height = WindowManager.LayoutParams.MATCH_PARENT
    attributes = lp
    WindowInsetsControllerCompat(this, this.decorView).apply {
        hide(WindowInsetsCompat.Type.statusBars())
        hide(WindowInsetsCompat.Type.navigationBars())
    }
}
