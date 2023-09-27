package com.ismartcoding.lib.extensions

import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager

val RecyclerView.LayoutManager?.firstVisibleItemPosition
    get() =
        when (this) {
            is LinearLayoutManager -> findFirstVisibleItemPosition()
            is StaggeredGridLayoutManager -> findFirstVisibleItemPositions(null)[0]
            else -> 0
        }

val RecyclerView.LayoutManager?.isLayoutReversed
    get() =
        when (this) {
            is LinearLayoutManager -> reverseLayout
            is StaggeredGridLayoutManager -> reverseLayout
            else -> false
        }
