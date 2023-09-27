package com.ismartcoding.plain.ui.views

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.widget.ScrollView
import kotlin.math.abs

class GoodScrollView(context: Context?, attrs: AttributeSet?) : ScrollView(context, attrs) {
    var onScrollChanged: ((l: Int, t: Int, oldl: Int, oldt: Int) -> Unit)? = null
    var lastY = 0
    var listenerEnabled = true

    override fun onScrollChanged(
        l: Int,
        t: Int,
        oldl: Int,
        oldt: Int,
    ) {
        super.onScrollChanged(l, t, oldl, oldt)
        if (onScrollChanged == null || !listenerEnabled) return
        if (abs(lastY - t) > 100) {
            lastY = t
            onScrollChanged?.invoke(l, t, oldl, oldt)
        }
    }

    fun tempDisableListener(mills: Int) {
        listenerEnabled = false
        Handler(Looper.getMainLooper()).postDelayed({ listenerEnabled = true }, mills.toLong())
    }
}
