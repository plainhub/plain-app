package com.ismartcoding.plain.ui.views

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.webkit.WebView

class CustomWebView(context: Context, attrs: AttributeSet?) : WebView(context, attrs) {
    private var refreshState: ((canRefresh: Boolean) -> Unit)? = null

    fun setRefreshStateListener(refreshState: ((canRefresh: Boolean) -> Unit)?) {
        this.refreshState = refreshState
    }

    override fun onOverScrolled(
        scrollX: Int,
        scrollY: Int,
        clampedX: Boolean,
        clampedY: Boolean,
    ) {
        super.onOverScrolled(scrollX, scrollY, clampedX, clampedY)
        refreshState?.invoke(clampedY)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (event.action == MotionEvent.ACTION_DOWN) {
            refreshState?.invoke(false)
        }
        return super.onTouchEvent(event)
    }
}
