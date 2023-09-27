package com.ismartcoding.lib.brv.item

interface ItemStableId {
    /**
     * 每个item数据模型都要实现本方法返回唯一ID
     * 请勿返回列表位置position
     */
    fun getItemId(): Long
}
