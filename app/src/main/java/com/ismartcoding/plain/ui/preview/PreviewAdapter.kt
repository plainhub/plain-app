package com.ismartcoding.plain.ui.preview

import android.view.View
import android.view.ViewGroup
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.ui.preview.viewholders.*

class PreviewAdapter(var key: String) : PagingDataAdapter<PreviewItem, RecyclerView.ViewHolder>(diff) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): RecyclerView.ViewHolder {
        return when (viewType) {
            ItemType.IMAGE -> SubsamplingViewHolder(parent)
            ItemType.VIDEO -> VideoViewHolder(parent)
            else -> UnknownViewHolder(View(parent.context))
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int,
    ) {
        getItem(position)?.let {
            when (holder) {
                is SubsamplingViewHolder -> holder.bind(it)
                is VideoViewHolder -> holder.bind(it)
            }

            if (it.id == key) {
                sendEvent(ViewerInitEvent(position, holder))
                key = ""
            }
        }
    }

    override fun getItemViewType(position: Int) = getItem(position)?.itemType(MainApp.instance) ?: ItemType.UNKNOWN
}

private val diff =
    object : DiffUtil.ItemCallback<PreviewItem>() {
        override fun areItemsTheSame(
            oldItem: PreviewItem,
            newItem: PreviewItem,
        ): Boolean {
            return newItem.id == oldItem.id
        }

        override fun areContentsTheSame(
            oldItem: PreviewItem,
            newItem: PreviewItem,
        ): Boolean {
            return newItem.id == oldItem.id
        }
    }
