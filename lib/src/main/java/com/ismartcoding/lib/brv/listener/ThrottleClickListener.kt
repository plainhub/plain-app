package com.ismartcoding.lib.brv.listener

import android.view.View

internal fun View.throttleClick(
    period: Long = 500,
    block: View.() -> Unit,
) {
    setOnClickListener(ThrottleClickListener(period, block))
}

private class ThrottleClickListener(private val period: Long = 500, private var block: View.() -> Unit) :
    View.OnClickListener {
    private var lastTime: Long = 0

    override fun onClick(v: View) {
        val currentTime = System.currentTimeMillis()
        if (currentTime - lastTime > period) {
            lastTime = currentTime
            block(v)
        }
    }
}
