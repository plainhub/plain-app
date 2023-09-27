package com.ismartcoding.lib.brv.listener

import androidx.recyclerview.widget.RecyclerView
import com.ismartcoding.lib.brv.BindingAdapter

interface OnBindViewHolderListener {
    fun onBindViewHolder(
        rv: RecyclerView,
        adapter: BindingAdapter,
        holder: BindingAdapter.BindingViewHolder,
        position: Int,
    )
}
