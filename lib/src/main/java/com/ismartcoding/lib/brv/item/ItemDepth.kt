package com.ismartcoding.lib.brv.item

import com.ismartcoding.lib.brv.item.ItemDepth.Companion.refreshItemDepth

/**
 * Item实现该接口来用于记录元素位于集合的层级深度[itemDepth]
 * @see refreshItemDepth 实现接口后还需要手动在任意位置调用一次该函数进行初始化赋值[ItemDepth.itemDepth]
 */
interface ItemDepth {
    /** 当前item在分组中的深度 */
    var itemDepth: Int

    companion object {
        /**
         * 递归遍历列表为所有实现[ItemDepth]的元素中的字段[ItemDepth.itemDepth]赋值当前位于集合的层级深度
         * @param initDepth  层级深度初始值
         */
        fun <T> refreshItemDepth(
            models: List<T>,
            initDepth: Int = 0,
        ): List<T> =
            models.onEach { item ->
                if (item is ItemDepth) {
                    item.itemDepth = initDepth
                }
                if (item is ItemExpand) {
                    item.itemSublist.run {
                        refreshItemDepth(this, initDepth + 1)
                    }
                }
            }
    }
}
