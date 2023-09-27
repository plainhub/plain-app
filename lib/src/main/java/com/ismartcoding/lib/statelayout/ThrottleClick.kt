package com.ismartcoding.lib.statelayout

import android.view.View
import java.util.concurrent.TimeUnit

internal fun View.throttleClick(
    interval: Long = 500,
    unit: TimeUnit = TimeUnit.MILLISECONDS,
    block: View.() -> Unit,
) {
    setOnClickListener(ThrottleClickListener(interval, unit, block))
}

internal class ThrottleClickListener(
    private val interval: Long = 500,
    private val unit: TimeUnit = TimeUnit.MILLISECONDS,
    private var block: View.() -> Unit,
) : View.OnClickListener {
    private var lastTime: Long = 0

    override fun onClick(v: View) {
        val currentTime = System.currentTimeMillis()

        if (currentTime - lastTime > unit.toMillis(interval)) {
            lastTime = currentTime
            block(v)
        }
    }
}
