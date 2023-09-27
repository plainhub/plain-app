package com.ismartcoding.plain.ui.extensions

import android.view.View
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import com.ismartcoding.plain.ui.helpers.SafeClickListener

fun View.alphaEnable(enable: Boolean) {
    if (enable) {
        this.alpha = 1.0f
        this.isEnabled = true
    } else {
        this.alpha = Constants.GRAY_OUT_ALPHA
        this.isEnabled = false
    }
}

fun View.setSafeClick(onSafeClick: (View) -> Unit) {
    setOnClickListener(
        SafeClickListener {
            onSafeClick(it)
        },
    )
}

fun View.actionBarItemBackground() {
    setBackgroundResource(R.drawable.action_bar_item_background)
}
