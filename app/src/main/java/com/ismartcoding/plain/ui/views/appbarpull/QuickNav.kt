package com.ismartcoding.plain.ui.views.appbarpull

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.core.view.updateLayoutParams
import com.google.android.material.appbar.AppBarLayout
import com.ismartcoding.lib.extensions.dp2px
import com.ismartcoding.plain.databinding.ViewQuickNavBinding
import com.ismartcoding.plain.ui.extensions.setSafeClick

class QuickNav(context: Context, attributeSet: AttributeSet? = null) : FrameLayout(context, attributeSet) {
    private val binding = ViewQuickNavBinding.inflate(LayoutInflater.from(context), this, true)
    var contentHeight = context.dp2px(400)

    init {
        binding.close.setSafeClick {
            val anim = ValueAnimator.ofInt(height, 0)
            anim.addUpdateListener { valueAnimator ->
                val v = valueAnimator.animatedValue as Int
                updateLayoutParams<AppBarLayout.LayoutParams> {
                    height = v
                }
            }
            anim.duration = 200
            anim.start()
        }
    }
}
