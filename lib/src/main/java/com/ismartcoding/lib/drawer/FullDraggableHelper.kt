package com.ismartcoding.lib.drawer

import android.annotation.SuppressLint
import android.content.Context
import android.view.*
import com.ismartcoding.lib.extensions.dp2px

class FullDraggableHelper(private val context: Context, private val callback: Callback) {
    private var initialMotionX = 0f
    private var initialMotionY = 0f
    private var lastMotionX = 0f
    private val touchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop
    private val swipeSlop: Int = context.dp2px(8)
    private val distanceThreshold: Int = context.dp2px(80)
    private val xVelocityThreshold: Int = context.dp2px(150)
    private var gravity = Gravity.NO_GRAVITY
    private var isDraggingDrawer = false
    private var shouldOpenDrawer = false
    private var velocityTracker: VelocityTracker? = null

    fun onInterceptTouchEvent(event: MotionEvent): Boolean {
        var intercepted = false
        val action = event.actionMasked
        val x = event.x
        val y = event.y
        if (action == MotionEvent.ACTION_DOWN) {
            initialMotionX = x
            lastMotionX = initialMotionX
            initialMotionY = y
            return false
        } else if (action == MotionEvent.ACTION_MOVE) {
            if (canNestedViewScroll(callback.drawerMainContainer, false, (x - lastMotionX).toInt(), x.toInt(), y.toInt())) {
                return false
            }
            lastMotionX = x
            val diffX = x - initialMotionX
            intercepted = Math.abs(diffX) > touchSlop && Math.abs(diffX) > Math.abs(y - initialMotionY) && isDrawerEnabled(diffX)
        }
        return intercepted
    }

    @SuppressLint("RtlHardcoded")
    fun onTouchEvent(event: MotionEvent): Boolean {
        val x = event.x
        val action = event.actionMasked
        when (action) {
            MotionEvent.ACTION_MOVE -> {
                val diffX = x - initialMotionX
                if (isDrawerOpen || !isDrawerEnabled(diffX)) {
                    return false
                }
                val absDiffX = Math.abs(diffX)
                if (absDiffX > swipeSlop || isDraggingDrawer) {
                    if (velocityTracker == null) {
                        velocityTracker = VelocityTracker.obtain()
                    }
                    velocityTracker!!.addMovement(event)
                    val lastDraggingDrawer = isDraggingDrawer
                    isDraggingDrawer = true
                    shouldOpenDrawer = absDiffX > distanceThreshold

                    // Not allowed to change direction in a process
                    if (gravity == Gravity.NO_GRAVITY) {
                        gravity = if (diffX > 0) Gravity.LEFT else Gravity.RIGHT
                    } else if (gravity == Gravity.LEFT && diffX < 0 || gravity == Gravity.RIGHT && diffX > 0) {
                        // Means that the motion first moves in one direction,
                        // and then completely close the drawer in the reverse direction.
                        // At this time, absDiffX should not be distributed anymore.
                        // So for this case, we are returning false,
                        // and set the initialMotionX to the direction changed point
                        // to support quick dragging out with the original direction.
                        initialMotionX = x
                        return false
                    }
                    callback.offsetDrawer(gravity, absDiffX - swipeSlop)
                    if (!lastDraggingDrawer) {
                        callback.onDrawerDragging()
                    }
                }
                return isDraggingDrawer
            }
            MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
                if (isDraggingDrawer) {
                    if (velocityTracker != null) {
                        velocityTracker!!.computeCurrentVelocity(1000)
                        val xVelocity = velocityTracker!!.xVelocity
                        val fromLeft = gravity == Gravity.LEFT
                        if (xVelocity > xVelocityThreshold) {
                            shouldOpenDrawer = fromLeft
                        } else if (xVelocity < -xVelocityThreshold) {
                            shouldOpenDrawer = !fromLeft
                        }
                    }
                    if (shouldOpenDrawer) {
                        callback.smoothOpenDrawer(gravity)
                    } else {
                        callback.smoothCloseDrawer(gravity)
                    }
                }
                shouldOpenDrawer = false
                isDraggingDrawer = false
                gravity = Gravity.NO_GRAVITY
                if (velocityTracker != null) {
                    velocityTracker!!.recycle()
                    velocityTracker = null
                }
            }
        }
        return true
    }

    private fun canNestedViewScroll(
        view: View,
        checkSelf: Boolean,
        dx: Int,
        x: Int,
        y: Int,
    ): Boolean {
        if (view is ViewGroup) {
            val group = view
            val scrollX = view.getScrollX()
            val scrollY = view.getScrollY()
            val count = group.childCount
            for (i in count - 1 downTo 0) {
                val child = group.getChildAt(i)
                if (child.visibility != View.VISIBLE) continue
                if (x + scrollX >= child.left && x + scrollX < child.right && y + scrollY >= child.top && y + scrollY < child.bottom &&
                    canNestedViewScroll(
                        child,
                        true,
                        dx,
                        x + scrollX - child.left,
                        y + scrollY - child.top,
                    )
                ) {
                    return true
                }
            }
        }
        return checkSelf && view.canScrollHorizontally(-dx)
    }

    @get:SuppressLint("RtlHardcoded")
    private val isDrawerOpen: Boolean
        get() = callback.isDrawerOpen(Gravity.LEFT) || callback.isDrawerOpen(Gravity.RIGHT)

    @SuppressLint("RtlHardcoded")
    private fun isDrawerEnabled(direction: Float): Boolean {
        return (
            direction > 0 && callback.hasEnabledDrawer(Gravity.LEFT) ||
                direction < 0 && callback.hasEnabledDrawer(Gravity.RIGHT)
        )
    }

    interface Callback {
        val drawerMainContainer: View

        fun isDrawerOpen(gravity: Int): Boolean

        fun hasEnabledDrawer(gravity: Int): Boolean

        fun offsetDrawer(
            gravity: Int,
            offset: Float,
        )

        fun smoothOpenDrawer(gravity: Int)

        fun smoothCloseDrawer(gravity: Int)

        fun onDrawerDragging()
    }
}
