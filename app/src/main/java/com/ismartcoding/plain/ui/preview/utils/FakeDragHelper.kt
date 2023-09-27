package com.ismartcoding.plain.ui.preview.utils

import android.content.Context
import android.view.View
import android.view.ViewConfiguration
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.ui.preview.ViewerDragEvent
import com.ismartcoding.plain.ui.preview.ViewerReleaseEvent
import com.ismartcoding.plain.ui.preview.ViewerRestoreEvent
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

class FakeDragHelper(val context: Context) {
    private var fakeDragOffset = 0f
    private var lastX = 0f
    private var lastY = 0f
    private val scaledTouchSlop by lazy { ViewConfiguration.get(context).scaledTouchSlop * Config.SWIPE_TOUCH_SLOP }

    fun drag(
        view: View,
        rawX: Float,
        rawY: Float,
    ) {
        if (lastX == 0f) lastX = rawX
        if (lastY == 0f) lastY = rawY
        val offsetX = rawX - lastX
        val offsetY = rawY - lastY
        if (fakeDragOffset == 0f) {
            if (offsetY > scaledTouchSlop) {
                fakeDragOffset = scaledTouchSlop
            } else if (offsetY < -scaledTouchSlop) {
                fakeDragOffset = -scaledTouchSlop
            }
        }
        if (fakeDragOffset != 0f) {
            val fixedOffsetY = offsetY - fakeDragOffset
            view.parent?.requestDisallowInterceptTouchEvent(true)
            val fraction = abs(max(-1f, min(1f, fixedOffsetY / view.height)))
            val fakeScale = 1 - min(0.4f, fraction)
            view.scaleX = fakeScale
            view.scaleY = fakeScale
            view.translationY = fixedOffsetY
            view.translationX = offsetX / 2
            sendEvent(ViewerDragEvent(view, fraction))
        }
    }

    fun up(
        view: View,
        setSingleTouch: (Boolean) -> Unit,
    ) {
        view.parent?.requestDisallowInterceptTouchEvent(false)
        setSingleTouch(true)
        fakeDragOffset = 0f
        lastX = 0f
        lastY = 0f

        val dismissEdge = view.height * Config.DISMISS_FRACTION
        if (abs(view.translationY) > dismissEdge) {
            sendEvent(ViewerReleaseEvent(view))
        } else {
            val offsetY = view.translationY
            val fraction = min(1f, offsetY / view.height)
            sendEvent(ViewerRestoreEvent(view, fraction))
            view.animate()
                .translationX(0f).translationY(0f).scaleX(1f).scaleY(1f)
                .setDuration(200).start()
        }
    }
}
