package com.ismartcoding.plain.ui.extensions

import androidx.annotation.ColorRes
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import com.google.android.material.appbar.AppBarLayout
import com.ismartcoding.plain.databinding.ViewTopAppBarBinding

fun ViewTopAppBarBinding.setScrollBehavior(enabled: Boolean) {
    val flags =
        if (enabled) {
            AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL or AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS
        } else {
            0
        }
    quickNav.updateLayoutParams<AppBarLayout.LayoutParams> {
        scrollFlags = flags
    }
    toolbar.updateLayoutParams<AppBarLayout.LayoutParams> {
        scrollFlags = flags
    }
    progressBar.updateLayoutParams<AppBarLayout.LayoutParams> {
        scrollFlags = flags
    }
    notification.updateLayoutParams<AppBarLayout.LayoutParams> {
        scrollFlags = flags
    }
}

fun ViewTopAppBarBinding.hideNotification() {
    notification.isVisible = false
}

fun ViewTopAppBarBinding.showNotification(
    text: String,
    @ColorRes colorId: Int,
) {
    notification.isVisible = true
    notification.setBackgroundColor(notification.context.getColor(colorId))
    notification.text = text
}
