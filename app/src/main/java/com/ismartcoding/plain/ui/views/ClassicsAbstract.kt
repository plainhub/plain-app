package com.ismartcoding.plain.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.scwang.smart.refresh.layout.api.*
import com.scwang.smart.refresh.layout.simple.SimpleComponent
import com.scwang.smart.refresh.layout.util.SmartUtil

abstract class ClassicsAbstract(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) :
    SimpleComponent(
        context,
        attrs,
        defStyleAttr,
    ),
    RefreshComponent {
    protected var mRefreshKernel: RefreshKernel? = null
    protected var mFinishDuration = 500
    protected var mPaddingTop = 20
    protected var mPaddingBottom = 20
    protected var mMinHeightOfContent = 0

    override fun onMeasure(
        widthMeasureSpec: Int,
        heightMeasureSpec: Int,
    ) {
        val thisView: View = this
        if (mMinHeightOfContent == 0) {
            mPaddingTop = thisView.paddingTop
            mPaddingBottom = thisView.paddingBottom
            if (mPaddingTop == 0 || mPaddingBottom == 0) {
                val paddingLeft = thisView.paddingLeft
                val paddingRight = thisView.paddingRight
                mPaddingTop = if (mPaddingTop == 0) SmartUtil.dp2px(20f) else mPaddingTop
                mPaddingBottom = if (mPaddingBottom == 0) SmartUtil.dp2px(20f) else mPaddingBottom
                thisView.setPadding(paddingLeft, mPaddingTop, paddingRight, mPaddingBottom)
            }
            this.clipToPadding = false
        }
        if (MeasureSpec.getMode(heightMeasureSpec) == MeasureSpec.EXACTLY) {
            val parentHeight: Int = MeasureSpec.getSize(heightMeasureSpec)
            if (parentHeight < mMinHeightOfContent) {
                val padding = (parentHeight - mMinHeightOfContent) / 2
                thisView.setPadding(thisView.paddingLeft, padding, thisView.paddingRight, padding)
            } else {
                thisView.setPadding(thisView.paddingLeft, 0, thisView.paddingRight, 0)
            }
        } else {
            thisView.setPadding(thisView.paddingLeft, mPaddingTop, thisView.paddingRight, mPaddingBottom)
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        if (mMinHeightOfContent == 0) {
            val thisGroup: ViewGroup = this
            for (i in 0 until thisGroup.childCount) {
                val height: Int = thisGroup.getChildAt(i).measuredHeight
                if (mMinHeightOfContent < height) {
                    mMinHeightOfContent = height
                }
            }
        }
    }

    override fun onInitialized(
        kernel: RefreshKernel,
        height: Int,
        maxDragHeight: Int,
    ) {
        mRefreshKernel = kernel
    }

    override fun onStartAnimator(
        refreshLayout: RefreshLayout,
        height: Int,
        maxDragHeight: Int,
    ) {
    }

    override fun onReleased(
        refreshLayout: RefreshLayout,
        height: Int,
        maxDragHeight: Int,
    ) {
        onStartAnimator(refreshLayout, height, maxDragHeight)
    }

    override fun onFinish(
        refreshLayout: RefreshLayout,
        success: Boolean,
    ): Int {
        return mFinishDuration // 延迟500毫秒之后再弹回
    }
}
