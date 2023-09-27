package com.ismartcoding.plain.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.ismartcoding.plain.Constants.CLICK_DRAG_TOLERANCE
import kotlin.math.abs

class MovableFloatingActionButton(context: Context, attrs: AttributeSet? = null) :
    FloatingActionButton(
        context,
        attrs,
    ),
    View.OnTouchListener {
    private var downRawX = 0f
    private var downRawY = 0f
    private var dX = 0f
    private var dY = 0f

    init {
        setOnTouchListener(this)
    }

    override fun onTouch(
        view: View,
        motionEvent: MotionEvent,
    ): Boolean {
        return when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                downRawX = motionEvent.rawX
                downRawY = motionEvent.rawY
                dX = view.x - downRawX
                dY = view.y - downRawY
                true // Consumed
            }
            MotionEvent.ACTION_MOVE -> {
                val viewWidth = view.width
                val viewHeight = view.height
                val viewParent = view.parent as View
                val parentWidth = viewParent.width
                val parentHeight = viewParent.height
                var newX = motionEvent.rawX + dX
                val layoutParams = view.layoutParams as MarginLayoutParams
                newX = layoutParams.leftMargin.toFloat().coerceAtLeast(newX) // Don't allow the FAB past the left hand side of the parent
                newX = (parentWidth - viewWidth - layoutParams.rightMargin).toFloat().coerceAtMost(newX) // Don't allow the FAB past the right hand side of the parent
                var newY = motionEvent.rawY + dY
                newY = layoutParams.topMargin.toFloat().coerceAtLeast(newY) // Don't allow the FAB past the top of the parent
                newY = (parentHeight - viewHeight - layoutParams.bottomMargin).toFloat().coerceAtMost(newY) // Don't allow the FAB past the bottom of the parent
                view.x = newX
                view.y = newY
                true // Consumed
            }
            MotionEvent.ACTION_UP -> {
                val upDX = motionEvent.rawX - downRawX
                val upDY = motionEvent.rawY - downRawY
                if (abs(upDX) < CLICK_DRAG_TOLERANCE && abs(upDY) < CLICK_DRAG_TOLERANCE) { // A click
                    performClick()
                } else { // A drag
                    true // Consumed
                }
            }
            else -> {
                super.onTouchEvent(motionEvent)
            }
        }
    }

    fun copyXY(
        x: Float,
        y: Float,
    ) {
        this.x = x
        this.y = y
    }
}
