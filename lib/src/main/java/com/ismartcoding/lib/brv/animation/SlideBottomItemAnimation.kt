package com.ismartcoding.lib.brv.animation

import android.animation.ObjectAnimator
import android.view.View

class SlideBottomItemAnimation : ItemAnimation {
    override fun onItemEnterAnimation(view: View) {
        ObjectAnimator.ofFloat(view, "translationY", view.measuredHeight.toFloat(), 0F)
            .setDuration(300)
            .start()
    }
}
