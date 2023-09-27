package com.ismartcoding.plain.ui.preview.views

import android.animation.ArgbEvaluator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.animation.DecelerateInterpolator
import androidx.constraintlayout.widget.ConstraintLayout
import com.ismartcoding.plain.ui.preview.utils.Config.DURATION_BG

class BackgroundView
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
    ) : ConstraintLayout(context, attrs) {
        private val argbEvaluator by lazy { ArgbEvaluator() }
        private var bgColor = Color.TRANSPARENT
        private var animator: ValueAnimator? = null

        fun changeToBackgroundColor(targetColor: Int) {
            animator =
                ValueAnimator.ofFloat(0f, 1f).apply {
                    duration = DURATION_BG
                    interpolator = DecelerateInterpolator()
                    val start = bgColor
                    addUpdateListener {
                        val fraction = it.animatedValue as Float
                        updateBackgroundColor(fraction, start, targetColor)
                    }
                }
            animator?.start()
        }

        fun updateBackgroundColor(
            fraction: Float,
            startValue: Int,
            endValue: Int,
        ) {
            setBackgroundColor(argbEvaluator.evaluate(fraction, startValue, endValue) as Int)
        }

        override fun setBackgroundColor(color: Int) {
            super.setBackgroundColor(color)
            bgColor = color
        }

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            animator?.cancel()
        }
    }
