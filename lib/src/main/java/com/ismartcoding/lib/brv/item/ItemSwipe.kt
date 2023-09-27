package com.ismartcoding.lib.brv.item

import com.ismartcoding.lib.brv.annotaion.ItemOrientation

/**
 * 可侧滑的条目
 */
interface ItemSwipe {
    /**
     * 侧滑方向
     * @see ItemOrientation
     */
    var itemOrientationSwipe: Int
}
