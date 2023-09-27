package com.ismartcoding.lib.brv.item

import com.ismartcoding.lib.brv.BindingAdapter

interface ItemAttached {
    fun onViewAttachedToWindow(holder: BindingAdapter.BindingViewHolder)

    fun onViewDetachedFromWindow(holder: BindingAdapter.BindingViewHolder)
}
