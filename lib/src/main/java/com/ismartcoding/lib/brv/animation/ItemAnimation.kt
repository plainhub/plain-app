package com.ismartcoding.lib.brv.animation

import android.view.View

interface ItemAnimation {
    /**
     * 处理item被添加的时候的进入动画
     */
    fun onItemEnterAnimation(view: View)
}
