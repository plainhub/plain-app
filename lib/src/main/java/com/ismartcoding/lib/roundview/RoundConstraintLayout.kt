package com.ismartcoding.lib.roundview

import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import androidx.constraintlayout.widget.ConstraintLayout

class RoundConstraintLayout
    @JvmOverloads
    constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = -1) :
    ConstraintLayout(
            context,
            attrs,
            defStyleAttr,
        ),
        RoundMethodInterface {
        override val helper: RoundHelper = RoundHelperImpl()

        init {
            helper.init(context, attrs, this)
        }

        override fun onSizeChanged(
            w: Int,
            h: Int,
            oldw: Int,
            oldh: Int,
        ) {
            super.onSizeChanged(w, h, oldw, oldh)
            helper.onSizeChanged(w, h)
        }

        override fun draw(canvas: Canvas) {
            helper.preDraw(canvas)
            super.draw(canvas)
            helper.drawPath(canvas, drawableState)
        }
    }
