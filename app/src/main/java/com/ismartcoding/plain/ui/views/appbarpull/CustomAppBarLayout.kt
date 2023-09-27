package com.ismartcoding.plain.ui.views.appbarpull

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import androidx.core.animation.doOnEnd
import androidx.core.view.updateLayoutParams
import com.google.android.material.appbar.AppBarLayout
import com.ismartcoding.plain.Constants
import com.ismartcoding.plain.R
import kotlin.math.abs

class CustomAppBarLayout(context: Context, attributeSet: AttributeSet? = null) : AppBarLayout(context, attributeSet) {
    lateinit var quickNav: QuickNav
    private var downRawY = 0f

    var dragToExpandEnabled = false // drag to expand the app bar.

    public override fun onFinishInflate() {
        super.onFinishInflate()
        quickNav = findViewById(R.id.quick_nav)
    }

    override fun onInterceptTouchEvent(motionEvent: MotionEvent): Boolean {
        if (!dragToExpandEnabled) {
            return false
        }
        when (motionEvent.action) {
            MotionEvent.ACTION_DOWN -> {
                downRawY = motionEvent.rawY
                return false
            }
            MotionEvent.ACTION_MOVE, MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                val upDY = motionEvent.rawY - downRawY
                return abs(upDY) > Constants.CLICK_DRAG_TOLERANCE
            }
        }
        return false
    }

    override fun onTouchEvent(motionEvent: MotionEvent): Boolean {
        return when (motionEvent.action) {
            MotionEvent.ACTION_MOVE -> {
                val y = motionEvent.rawY - downRawY
                if (dragToExpandEnabled && isReadyForPullDown(y)) {
                    if (quickNav.height == 0) {
                        val anim = ValueAnimator.ofInt(0, quickNav.contentHeight)
                        anim.addUpdateListener { valueAnimator ->
                            val v = valueAnimator.animatedValue as Int
                            quickNav.updateLayoutParams<LayoutParams> {
                                height = v
                                this@CustomAppBarLayout.requestLayout()
                            }
                        }
                        anim.duration = 200
                        anim.start()
                    }
                    true
                } else if (isReadyForPullUp(y)) {
                    if (quickNav.height > 0) {
                        val anim = ValueAnimator.ofInt(quickNav.height, 0)
                        anim.doOnEnd {
                            setExpanded(true)
                        }
                        anim.addUpdateListener { valueAnimator ->
                            val v = valueAnimator.animatedValue as Int
                            quickNav.updateLayoutParams<LayoutParams> {
                                height = v
                                this@CustomAppBarLayout.requestLayout()
                            }
                        }
                        anim.duration = 200
                        anim.start()
                    }
                    true
                } else {
                    false
                }
            }
            else -> false
        }
    }

    private fun isReadyForPullDown(y: Float): Boolean {
        return y > 0
    }

    private fun isReadyForPullUp(y: Float): Boolean {
        return y < 0
    }
}
