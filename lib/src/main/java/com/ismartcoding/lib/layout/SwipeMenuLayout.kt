package com.ismartcoding.lib.layout

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.*
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import com.ismartcoding.lib.R
import kotlin.math.abs

class SwipeMenuLayout(context: Context, attrs: AttributeSet? = null) : ViewGroup(context, attrs) {
    private var mScaleTouchSlop = 0
    private var mMaxVelocity = 0
    private var mPointerId = 0
    private var mHeight = 0
    private var mRightMenuWidths = 0
    private var mLimit = 0
    private var mContentView: View? = null
    private val mLastP = PointF()
    private var isUnMoved = true
    private val mFirstP = PointF()
    private var isUserSwiped = false
    private var mVelocityTracker: VelocityTracker? = null
    var isSwipeEnable = false
    var isLeftSwipe = true

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        isClickable = true
        mRightMenuWidths = 0
        mHeight = 0
        var contentWidth = 0
        val childCount = childCount
        val measureMatchParentChildren = MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY
        var isNeedMeasureChildHeight = false
        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            childView.isClickable = true
            if (childView.visibility != GONE) {
                measureChild(childView, widthMeasureSpec, heightMeasureSpec)
                val lp = childView.layoutParams as MarginLayoutParams
                mHeight = Math.max(mHeight, childView.measuredHeight)
                if (measureMatchParentChildren && lp.height == LayoutParams.MATCH_PARENT) {
                    isNeedMeasureChildHeight = true
                }
                if (i > 0) {
                    mRightMenuWidths += childView.measuredWidth
                } else {
                    mContentView = childView
                    contentWidth = childView.measuredWidth
                }
            }
        }
        setMeasuredDimension(
            paddingLeft + paddingRight + contentWidth,
            mHeight + paddingTop + paddingBottom,
        )
        mLimit = mRightMenuWidths * 4 / 10
        if (isNeedMeasureChildHeight) {
            forceUniformHeight(childCount, widthMeasureSpec)
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet): LayoutParams {
        return MarginLayoutParams(context, attrs)
    }

    private fun forceUniformHeight(
        count: Int,
        widthMeasureSpec: Int,
    ) {
        val uniformMeasureSpec =
            MeasureSpec.makeMeasureSpec(
                measuredHeight,
                MeasureSpec.EXACTLY,
            )
        for (i in 0 until count) {
            val child = getChildAt(i)
            if (child.visibility != GONE) {
                val lp = child.layoutParams as MarginLayoutParams
                if (lp.height == LayoutParams.MATCH_PARENT) {
                    val oldWidth = lp.width
                    lp.width = child.measuredWidth
                    measureChildWithMargins(child, widthMeasureSpec, 0, uniformMeasureSpec, 0)
                    lp.width = oldWidth
                }
            }
        }
    }

    override fun onLayout(
        changed: Boolean,
        l: Int,
        t: Int,
        r: Int,
        b: Int,
    ) {
        val childCount = childCount
        var left = 0 + paddingLeft
        var right = 0 + paddingLeft
        for (i in 0 until childCount) {
            val childView = getChildAt(i)
            if (childView.visibility != GONE) {
                if (i == 0) {
                    childView.layout(left, paddingTop, left + childView.measuredWidth, paddingTop + childView.measuredHeight)
                    left += childView.measuredWidth
                } else {
                    if (isLeftSwipe) {
                        childView.layout(left, paddingTop, left + childView.measuredWidth, paddingTop + childView.measuredHeight)
                        left += childView.measuredWidth
                    } else {
                        childView.layout(right - childView.measuredWidth, paddingTop, right, paddingTop + childView.measuredHeight)
                        right -= childView.measuredWidth
                    }
                }
            }
        }
    }

    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (isSwipeEnable) {
            acquireVelocityTracker(ev)
            val verTracker = mVelocityTracker
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    isUserSwiped = false
                    isUnMoved = true
                    if (isTouching) {
                        return false
                    } else {
                        isTouching = true
                    }
                    mLastP[ev.rawX] = ev.rawY
                    mFirstP[ev.rawX] = ev.rawY
                    if (viewCache != null) {
                        if (viewCache !== this) {
                            viewCache?.smoothClose()
                        }
                        parent.requestDisallowInterceptTouchEvent(true)
                    }
                    mPointerId = ev.getPointerId(0)
                }
                MotionEvent.ACTION_MOVE -> {
                    val gap = mLastP.x - ev.rawX
                    if (Math.abs(gap) > 10 || Math.abs(scrollX) > 10) {
                        parent.requestDisallowInterceptTouchEvent(true)
                    }
                    if (Math.abs(gap) > mScaleTouchSlop) {
                        isUnMoved = false
                    }
                    scrollBy(gap.toInt(), 0)
                    if (isLeftSwipe) {
                        if (scrollX < 0) {
                            scrollTo(0, 0)
                        }
                        if (scrollX > mRightMenuWidths) {
                            scrollTo(mRightMenuWidths, 0)
                        }
                    } else {
                        if (scrollX < -mRightMenuWidths) {
                            scrollTo(-mRightMenuWidths, 0)
                        }
                        if (scrollX > 0) {
                            scrollTo(0, 0)
                        }
                    }
                    mLastP[ev.rawX] = ev.rawY
                }
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    if (abs(ev.rawX - mFirstP.x) > mScaleTouchSlop) {
                        isUserSwiped = true
                    }
                    verTracker!!.computeCurrentVelocity(1000, mMaxVelocity.toFloat())
                    val velocityX = verTracker.getXVelocity(mPointerId)
                    if (Math.abs(velocityX) > 1000) {
                        if (velocityX < -1000) {
                            if (isLeftSwipe) {
                                smoothExpand()
                            } else {
                                smoothClose()
                            }
                        } else {
                            if (isLeftSwipe) {
                                smoothClose()
                            } else {
                                smoothExpand()
                            }
                        }
                    } else {
                        if (Math.abs(scrollX) > mLimit) {
                            smoothExpand()
                        } else {
                            smoothClose()
                        }
                    }
                    releaseVelocityTracker()
                    isTouching = false
                }
                else -> {}
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isSwipeEnable) {
            when (ev.action) {
                MotionEvent.ACTION_MOVE ->
                    if (Math.abs(ev.rawX - mFirstP.x) > mScaleTouchSlop) {
                        return true
                    }
                MotionEvent.ACTION_UP -> {
                    if (isLeftSwipe) {
                        if (scrollX > mScaleTouchSlop) {
                            if (ev.x < width - scrollX) {
                                if (isUnMoved) {
                                    smoothClose()
                                }
                                return true // true表示拦截
                            }
                        }
                    } else {
                        if (-scrollX > mScaleTouchSlop) {
                            if (ev.x > -scrollX) {
                                if (isUnMoved) {
                                    smoothClose()
                                }
                                return true
                            }
                        }
                    }
                    if (isUserSwiped) {
                        return true
                    }
                }
            }
        }
        return super.onInterceptTouchEvent(ev)
    }

    private var mExpandAnim: ValueAnimator? = null
    private var mCloseAnim: ValueAnimator? = null
    var isExpand = false

    fun smoothExpand() {
        viewCache = this@SwipeMenuLayout
        mContentView?.isLongClickable = false
        cancelAnim()
        mExpandAnim = ValueAnimator.ofInt(scrollX, if (isLeftSwipe) mRightMenuWidths else -mRightMenuWidths)
        mExpandAnim?.addUpdateListener { animation: ValueAnimator -> scrollTo((animation.animatedValue as Int), 0) }
        mExpandAnim?.setInterpolator(OvershootInterpolator())
        mExpandAnim?.addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isExpand = true
                }
            },
        )
        mExpandAnim?.setDuration(300)?.start()
    }

    private fun cancelAnim() {
        if (mCloseAnim?.isRunning == true) {
            mCloseAnim?.cancel()
        }
        if (mExpandAnim?.isRunning == true) {
            mExpandAnim?.cancel()
        }
    }

    fun smoothClose() {
        viewCache = null
        mContentView?.isLongClickable = true
        cancelAnim()
        mCloseAnim = ValueAnimator.ofInt(scrollX, 0)
        mCloseAnim?.addUpdateListener { animation -> scrollTo((animation.animatedValue as Int), 0) }
        mCloseAnim?.interpolator = AccelerateInterpolator()
        mCloseAnim?.addListener(
            object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    isExpand = false
                }
            },
        )
        mCloseAnim?.setDuration(300)?.start()
    }

    private fun acquireVelocityTracker(event: MotionEvent) {
        if (null == mVelocityTracker) {
            mVelocityTracker = VelocityTracker.obtain()
        }
        mVelocityTracker?.addMovement(event)
    }

    private fun releaseVelocityTracker() {
        mVelocityTracker?.clear()
        mVelocityTracker?.recycle()
        mVelocityTracker = null
    }

    override fun onDetachedFromWindow() {
        if (this === viewCache) {
            viewCache?.smoothClose()
            viewCache = null
        }
        super.onDetachedFromWindow()
    }

    override fun performLongClick(): Boolean {
        return if (Math.abs(scrollX) > mScaleTouchSlop) {
            false
        } else {
            super.performLongClick()
        }
    }

    fun quickClose() {
        if (this === viewCache) {
            cancelAnim()
            viewCache?.scrollTo(0, 0)
            viewCache = null
        }
    }

    companion object {
        var viewCache: SwipeMenuLayout? = null
            private set
        private var isTouching = false
    }

    init {
        val a = context.obtainStyledAttributes(attrs, R.styleable.SwipeMenuLayout)
        isSwipeEnable = a.getBoolean(R.styleable.SwipeMenuLayout_swipeEnable, false)
        a.recycle()
        mScaleTouchSlop = ViewConfiguration.get(context).scaledTouchSlop
        mMaxVelocity = ViewConfiguration.get(context).scaledMaximumFlingVelocity
    }
}
