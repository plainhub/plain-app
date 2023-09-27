package com.ismartcoding.lib.brv.listener

import android.graphics.Canvas
import android.view.View
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.ismartcoding.lib.R
import com.ismartcoding.lib.brv.BindingAdapter
import com.ismartcoding.lib.brv.annotaion.ItemOrientation
import com.ismartcoding.lib.brv.item.ItemDrag
import com.ismartcoding.lib.brv.item.ItemSwipe
import com.ismartcoding.lib.brv.utils.bindingAdapter
import com.ismartcoding.lib.layout.SwipeMenuLayout

/**
 * 默认实现拖拽替换和侧滑删除
 */
open class DefaultItemTouchCallback : ItemTouchHelper.Callback() {
    /** 侧滑到底item消失时 */
    override fun onSwiped(
        viewHolder: RecyclerView.ViewHolder,
        direction: Int,
    ) {
        val adapter = viewHolder.bindingAdapter as? BindingAdapter
        val layoutPosition = viewHolder.layoutPosition
        adapter?.notifyItemRemoved(layoutPosition)
        (adapter?.models as ArrayList).removeAt(layoutPosition)
    }

    /**
     * 返回值表示拖拽/侧滑的方向
     * @param viewHolder 拖拽触发的Item
     */
    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
    ): Int {
        var drag = 0
        var swipe = 0
        if (viewHolder is BindingAdapter.BindingViewHolder) {
            val model = viewHolder.getModel<Any>()
            if (model is ItemDrag) {
                val v = viewHolder.itemView.findViewById<SwipeMenuLayout>(R.id.swipe_menu)
                drag =
                    if (v?.isExpand == true) {
                        ItemOrientation.NONE
                    } else {
                        model.itemOrientationDrag
                    }
            }
            if (model is ItemSwipe) swipe = model.itemOrientationSwipe
        }
        return makeMovementFlags(drag, swipe)
    }

    /** 绘制拖拽或者侧滑动画 */
    override fun onChildDraw(
        c: Canvas,
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        dX: Float,
        dY: Float,
        actionState: Int,
        isCurrentlyActive: Boolean,
    ) {
        if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
            val swipeView = viewHolder.itemView.findViewWithTag<View>("swipe")
            if (swipeView != null) {
                swipeView.translationX = dX
            } else {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
            }
        } else {
            super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
        }
    }

    /**
     * 滑动距离速率来判断当前是否执行滑动删除事件(可以理解为移出itemView)
     * @param viewHolder 拖拽触发的Item
     */
    override fun getSwipeThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 1f
    }

    override fun clearView(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
    ) {
        super.clearView(recyclerView, viewHolder)
        val view = viewHolder.itemView.findViewWithTag<View>("swipe")
        if (view != null) {
            view.translationX = 0F
        }
    }

    /**
     * 当拖拽动作完成且松开手指时触发
     * @param source 触发拖拽的Item
     * @param target 拖拽目标的Item
     */
    open fun onDrag(
        source: BindingAdapter.BindingViewHolder,
        target: BindingAdapter.BindingViewHolder,
    ) {
    }

    private var lastActionState: Int = 0
    private var sourceViewHolder: BindingAdapter.BindingViewHolder? = null
    private var targetViewHolder: BindingAdapter.BindingViewHolder? = null

    /**
     * 状态变化
     * @param actionState
     * @see ItemTouchHelper.ACTION_STATE_DRAG 拖拽
     * @see ItemTouchHelper.ACTION_STATE_SWIPE 侧滑
     * @see ItemTouchHelper.ACTION_STATE_IDLE 闲置
     */
    override fun onSelectedChanged(
        viewHolder: RecyclerView.ViewHolder?,
        actionState: Int,
    ) {
        when (actionState) {
            ItemTouchHelper.ACTION_STATE_IDLE -> {
                if (lastActionState == ItemTouchHelper.ACTION_STATE_DRAG &&
                    sourceViewHolder is BindingAdapter.BindingViewHolder &&
                    targetViewHolder is BindingAdapter.BindingViewHolder
                ) {
                    onDrag(sourceViewHolder!!, targetViewHolder!!)
                }
            }
            else -> {
                this.lastActionState = actionState
            }
        }
    }

    /** 拖拽移动超过其他item时, 其返回值表示是否已经拖拽替换(会触发函数onMoved) */
    override fun onMove(
        recyclerView: RecyclerView,
        source: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder,
    ): Boolean {
        val adapter = recyclerView.bindingAdapter as? BindingAdapter ?: return false
        val currentPosition = recyclerView.getChildLayoutPosition(source.itemView)
        val targetPosition = recyclerView.getChildLayoutPosition(target.itemView)

        if (source is BindingAdapter.BindingViewHolder && target is BindingAdapter.BindingViewHolder) {
            val model = target.getModel<Any>()
            if (model is ItemDrag && model.itemOrientationDrag != 0) {
                val fromPosition = currentPosition - adapter.headerCount
                val toPosition = targetPosition - adapter.headerCount
                val fromItem = adapter.mutable[fromPosition]
                adapter.mutable.apply {
                    removeAt(fromPosition)
                    add(toPosition, fromItem)
                }
                adapter.notifyItemMoved(currentPosition, targetPosition)
                sourceViewHolder = source
                targetViewHolder = target
            }
        }
        return false
    }
}
