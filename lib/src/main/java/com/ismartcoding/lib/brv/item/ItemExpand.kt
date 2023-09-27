package com.ismartcoding.lib.brv.item

/**
 * 可展开/折叠的条目
 */
interface ItemExpand {
    /** 同级别的分组的索引位置 */
    var itemGroupPosition: Int

    /** 是否已展开 */
    var itemExpand: Boolean

    /** 子列表 */
    var itemSublist: List<Any>
}
