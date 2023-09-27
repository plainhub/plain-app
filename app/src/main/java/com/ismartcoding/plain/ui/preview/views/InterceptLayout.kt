package com.ismartcoding.plain.ui.preview.views

import android.content.Context
import android.util.AttributeSet
import android.view.MotionEvent
import android.widget.FrameLayout
import com.ismartcoding.plain.ui.preview.utils.TransitionEndHelper
import com.ismartcoding.plain.ui.preview.utils.TransitionStartHelper

class InterceptLayout
    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) :
    FrameLayout(context, attrs, defStyleAttr) {
        override fun onInterceptTouchEvent(ev: MotionEvent?) =
            TransitionStartHelper.transitionAnimating || TransitionEndHelper.transitionAnimating
    }
