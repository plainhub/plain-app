package com.ismartcoding.lib.brv.item

import com.ismartcoding.lib.brv.BindingAdapter

/**
 * 实现接口进行UI操作不符合MVVM, 因为Model中不允许出现View引用. 并不是太推荐使用
 */
interface ItemBind {
    fun onBind(holder: BindingAdapter.BindingViewHolder)
}
