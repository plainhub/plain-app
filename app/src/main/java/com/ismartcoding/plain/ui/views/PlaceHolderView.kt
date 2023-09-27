package com.ismartcoding.plain.ui.views

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.view.animation.AnimationUtils
import com.ismartcoding.plain.R

class PlaceHolderView(context: Context, attrs: AttributeSet?) : View(context, attrs) {
    init {
        setBackgroundResource(R.drawable.loading_placeholder)
        startAnimation(AnimationUtils.loadAnimation(context, R.anim.placeholder))
    }
}
