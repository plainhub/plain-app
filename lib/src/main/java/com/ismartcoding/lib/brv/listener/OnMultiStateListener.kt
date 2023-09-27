package com.ismartcoding.lib.brv.listener

import com.scwang.smart.refresh.layout.api.RefreshFooter
import com.scwang.smart.refresh.layout.api.RefreshHeader
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.constant.RefreshState
import com.scwang.smart.refresh.layout.listener.OnMultiListener

open class OnMultiStateListener : OnMultiListener {
    override fun onFooterMoving(
        footer: RefreshFooter?,
        isDragging: Boolean,
        percent: Float,
        offset: Int,
        footerHeight: Int,
        maxDragHeight: Int,
    ) {
    }

    override fun onHeaderStartAnimator(
        header: RefreshHeader?,
        headerHeight: Int,
        maxDragHeight: Int,
    ) {
    }

    override fun onFooterReleased(
        footer: RefreshFooter?,
        footerHeight: Int,
        maxDragHeight: Int,
    ) {
    }

    override fun onStateChanged(
        refreshLayout: RefreshLayout,
        oldState: RefreshState,
        newState: RefreshState,
    ) {
    }

    override fun onHeaderMoving(
        header: RefreshHeader?,
        isDragging: Boolean,
        percent: Float,
        offset: Int,
        headerHeight: Int,
        maxDragHeight: Int,
    ) {
    }

    override fun onFooterFinish(
        footer: RefreshFooter?,
        success: Boolean,
    ) {
    }

    override fun onFooterStartAnimator(
        footer: RefreshFooter?,
        footerHeight: Int,
        maxDragHeight: Int,
    ) {
    }

    override fun onHeaderReleased(
        header: RefreshHeader?,
        headerHeight: Int,
        maxDragHeight: Int,
    ) {
    }

    override fun onLoadMore(refreshLayout: RefreshLayout) {
    }

    override fun onRefresh(refreshLayout: RefreshLayout) {
    }

    override fun onHeaderFinish(
        header: RefreshHeader?,
        success: Boolean,
    ) {
    }
}
