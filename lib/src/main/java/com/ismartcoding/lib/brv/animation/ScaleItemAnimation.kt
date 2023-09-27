package com.ismartcoding.lib.brv.animation

import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View

class ScaleItemAnimation
    @JvmOverloads
    constructor(private val mFrom: Float = DEFAULT_SCALE_FROM) : ItemAnimation {
        override fun onItemEnterAnimation(view: View) {
            val scaleX = ObjectAnimator.ofFloat(view, "scaleX", mFrom, 1f)
            val scaleY = ObjectAnimator.ofFloat(view, "scaleY", mFrom, 1f)
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(scaleX, scaleY)
            animatorSet.duration = 300
            animatorSet.start()
        }

        companion object {
            private val DEFAULT_SCALE_FROM = .5f
        }
    }
