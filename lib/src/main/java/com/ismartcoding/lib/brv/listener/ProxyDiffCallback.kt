package com.ismartcoding.lib.brv.listener

import androidx.recyclerview.widget.DiffUtil

/**
 * 将数据对比实现转交给[ItemDifferCallback]
 * @param newModels 新的数据
 * @param oldModels 旧的数据
 * @param callback 实际对比数据接口
 */
internal class ProxyDiffCallback(
    private val newModels: List<Any?>?,
    private val oldModels: List<Any?>?,
    val callback: ItemDifferCallback,
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int {
        return oldModels?.size ?: 0
    }

    override fun getNewListSize(): Int {
        return newModels?.size ?: 0
    }

    override fun areItemsTheSame(
        oldItemPosition: Int,
        newItemPosition: Int,
    ): Boolean {
        return if (oldModels == null || newModels == null) {
            false
        } else {
            val oldItem = oldModels[oldItemPosition]
            val newItem = newModels[newItemPosition]
            if (oldItem != null && newItem != null) {
                callback.areItemsTheSame(oldItem, newItem)
            } else {
                oldItem == null && newItem == null
            }
        }
    }

    override fun areContentsTheSame(
        oldItemPosition: Int,
        newItemPosition: Int,
    ): Boolean {
        return if (oldModels == null || newModels == null) {
            false
        } else {
            val oldItem = oldModels[oldItemPosition]
            val newItem = newModels[newItemPosition]
            if (oldItem != null && newItem != null) {
                callback.areContentsTheSame(oldItem, newItem)
            } else {
                oldItem == null && newItem == null
            }
        }
    }

    override fun getChangePayload(
        oldItemPosition: Int,
        newItemPosition: Int,
    ): Any? {
        return if (oldModels == null || newModels == null) {
            null
        } else {
            val oldItem = oldModels[oldItemPosition]
            val newItem = newModels[newItemPosition]
            if (oldItem != null && newItem != null) {
                callback.getChangePayload(oldItem, newItem)
            } else {
                null
            }
        }
    }
}
