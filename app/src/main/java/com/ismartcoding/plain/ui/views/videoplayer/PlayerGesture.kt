package com.ismartcoding.plain.ui.views.videoplayer

import android.content.Context
import android.view.GestureDetector
import android.view.MotionEvent
import kotlin.math.abs

class PlayerGesture(context: Context) : GestureDetector.OnGestureListener {
    /** 手势检测器 */
    private val mGestureDetector = GestureDetector(context.applicationContext, this)

    /** 触摸控件的宽度 */
    private var width = 0

    var onSingleTapUp: ((MotionEvent) -> Boolean)? = null
    var onScrollLeftRight: ((width: Int, distance: Float) -> Unit)? = null
    var onFingerDown: (() -> Unit)? = null
    var onFingerUp: (() -> Unit)? = null

    /**
     * 触摸事件
     *
     * @param event
     * @param viewWidth
     * @return
     */
    fun onTouchEvent(
        event: MotionEvent,
        viewWidth: Int,
    ): Boolean {
        width = viewWidth
        val action = event.action
        if (action == MotionEvent.ACTION_DOWN) {
            onFingerDown?.invoke()
        } else if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            onFingerUp?.invoke()
        }
        mGestureDetector.onTouchEvent(event)
        return true
    }

    override fun onShowPress(e: MotionEvent) {
    }

    override fun onSingleTapUp(e: MotionEvent): Boolean {
        return onSingleTapUp?.invoke(e) ?: false
    }

    override fun onDown(e: MotionEvent): Boolean {
        return false
    }

    override fun onFling(
        p0: MotionEvent?,
        p1: MotionEvent,
        p2: Float,
        p3: Float,
    ): Boolean {
        return false
    }

    override fun onScroll(
        p0: MotionEvent?,
        p1: MotionEvent,
        p2: Float,
        p3: Float,
    ): Boolean {
        val absDistanceX = abs(p2) // distanceX < 0 从左到右
        val absDistanceY = abs(p3) // distanceY < 0 从上到下
        if (absDistanceX < absDistanceY) { // Y方向的速率比X方向的大，即 上下 滑动
            return true
        }

        return false
    }

    override fun onLongPress(p0: MotionEvent) {
    }
}
