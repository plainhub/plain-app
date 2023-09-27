package com.ismartcoding.lib.brv.utils

import androidx.annotation.DrawableRes
import androidx.annotation.IntRange
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.VERTICAL
import com.ismartcoding.lib.brv.BindingAdapter
import com.ismartcoding.lib.brv.BindingAdapter.Companion.equals
import com.ismartcoding.lib.brv.DefaultDecoration
import com.ismartcoding.lib.brv.annotaion.DividerOrientation
import com.ismartcoding.lib.brv.layoutmanager.HoverGridLayoutManager
import com.ismartcoding.lib.brv.layoutmanager.HoverLinearLayoutManager
import com.ismartcoding.lib.brv.layoutmanager.HoverStaggeredGridLayoutManager
import com.ismartcoding.lib.brv.listener.ItemDifferCallback

// <editor-fold desc="数据集">

/**
 * 如果Adapter是[BindingAdapter]则返回对象, 否则抛出异常
 * @exception NullPointerException
 */
val RecyclerView.bindingAdapter
    get() =
        adapter as? BindingAdapter
            ?: throw NullPointerException("RecyclerView without BindingAdapter")

/**
 * 数据模型集合
 * 如果赋值的是[List]不可变集合将会自动被替换成[MutableList], 将无法保持为同一个集合对象引用
 */
var RecyclerView.models
    get() = bindingAdapter.models
    set(value) {
        bindingAdapter.models = value
    }

/**
 * 可增删的数据模型集合, 本质上就是返回可变的models. 假设未赋值给models则将抛出异常为[ClassCastException]
 */
var RecyclerView.mutable
    get() = bindingAdapter.models as? ArrayList ?: throw NullPointerException("[BindingAdapter.models] is null, no data")
    set(value) {
        bindingAdapter.models = value
    }

@Suppress("UNCHECKED_CAST")
fun <M> RecyclerView.getModelList(): List<M> {
    return bindingAdapter.getModelList()
}
// </editor-fold>

/**
 * 添加新的数据
 * @param models 被添加的数据
 * @param animation 是否使用动画
 * @param index 插入到[models]指定位置, 如果index超过[models]长度则会添加到最后
 */
fun RecyclerView.addModels(
    models: List<Any>?,
    animation: Boolean = true,
    @IntRange(from = -1) index: Int = -1,
) {
    bindingAdapter.addModels(models, animation, index)
}

/**
 * 对比数据, 根据数据差异自动刷新列表
 * 数据对比默认使用`equals`函数对比, 你可以为数据手动实现equals函数来修改对比逻辑. 推荐定义数据为 data class, 因其会根据构造参数自动生成equals
 * 如果数据集合很大导致对比速度很慢, 建议在非主步线程中调用此函数, 效果等同于[androidx.recyclerview.widget.AsyncListDiffer]
 *
 * 对于数据是否匹配可能需要你自定义[BindingAdapter.itemDifferCallback], 因为默认使用数据模型的[equals]方法匹配, 具体请阅读[ItemDifferCallback.DEFAULT]
 *
 * @param newModels 新的数据, 将覆盖旧的数据
 * @param detectMoves 是否对比Item的移动, true会导致列表当前位置发生移动
 * @param commitCallback 因为子线程调用[setDifferModels]刷新列表会不同步(刷新列表需要切换到主线程), 而[commitCallback]保证在刷新列表完成以后调用(运行在主线程)
 */
fun RecyclerView.setDifferModels(
    newModels: List<Any>?,
    detectMoves: Boolean = true,
    commitCallback: Runnable? = null,
) {
    bindingAdapter.setDifferModels(newModels, detectMoves, commitCallback)
}

// <editor-fold desc="配置列表">

/**
 * 设置适配器
 */
fun RecyclerView.setup(block: BindingAdapter.(RecyclerView) -> Unit): BindingAdapter {
    val adapter = BindingAdapter()
    adapter.block(this)
    this.adapter = adapter
    return adapter
}
// </editor-fold>

// <editor-fold desc="布局管理器">

/**
 * 创建[HoverLinearLayoutManager]  线性列表
 * @param orientation 列表方向
 * @param reverseLayout 是否反转列表
 * @param scrollEnabled 是否允许滚动
 */
fun RecyclerView.linear(
    @RecyclerView.Orientation orientation: Int = VERTICAL,
    reverseLayout: Boolean = false,
    scrollEnabled: Boolean = true,
    stackFromEnd: Boolean = false,
): RecyclerView {
    layoutManager =
        HoverLinearLayoutManager(context, orientation, reverseLayout).apply {
            setScrollEnabled(scrollEnabled)
            this.stackFromEnd = stackFromEnd
        }
    return this
}

/**
 * 创建[HoverGridLayoutManager] 网格列表
 * @param spanCount 网格跨度数量
 * @param orientation 列表方向
 * @param reverseLayout 是否反转
 * @param scrollEnabled 是否允许滚动
 */
fun RecyclerView.grid(
    spanCount: Int = 1,
    @RecyclerView.Orientation orientation: Int = VERTICAL,
    reverseLayout: Boolean = false,
    scrollEnabled: Boolean = true,
): RecyclerView {
    layoutManager =
        HoverGridLayoutManager(context, spanCount, orientation, reverseLayout).apply {
            setScrollEnabled(scrollEnabled)
        }
    return this
}

/**
 * 创建[HoverStaggeredGridLayoutManager] 交错列表
 * @param spanCount 网格跨度数量
 * @param orientation 列表方向
 * @param reverseLayout 是否反转
 * @param scrollEnabled 是否允许滚动
 */
fun RecyclerView.staggered(
    spanCount: Int,
    @RecyclerView.Orientation orientation: Int = VERTICAL,
    reverseLayout: Boolean = false,
    scrollEnabled: Boolean = true,
): RecyclerView {
    layoutManager =
        HoverStaggeredGridLayoutManager(spanCount, orientation).apply {
            setScrollEnabled(scrollEnabled)
            this.reverseLayout = reverseLayout
        }
    return this
}
// </editor-fold>

// <editor-fold desc="分割线">

/**
 * 函数配置分割线
 * 具体配置参数查看[DefaultDecoration]
 */
fun RecyclerView.divider(block: DefaultDecoration.() -> Unit): RecyclerView {
    val itemDecoration = DefaultDecoration(context).apply(block)
    addItemDecoration(itemDecoration)
    return this
}

/**
 * 指定Drawable资源为分割线, 分割线的间距和宽度应在资源文件中配置
 * @param drawable 描述分割线的drawable
 * @param orientation 分割线方向, 仅[androidx.recyclerview.widget.GridLayoutManager]需要使用此参数, 其他LayoutManager都是根据其方向自动推断
 */
fun RecyclerView.divider(
    @DrawableRes drawable: Int,
    orientation: DividerOrientation = DividerOrientation.HORIZONTAL,
): RecyclerView {
    return divider {
        setDrawable(drawable)
        this.orientation = orientation
    }
}

/**
 * 设置空白间距分割
 * @param space item的空白间距
 * @param orientation 分割线方向, 仅[androidx.recyclerview.widget.GridLayoutManager]需要使用此参数, 其他LayoutManager都是根据其方向自动推断
 */
fun RecyclerView.dividerSpace(
    space: Int,
    orientation: DividerOrientation = DividerOrientation.HORIZONTAL,
): RecyclerView {
    return divider {
        setDivider(space)
        this.orientation = orientation
    }
}
// </editor-fold>

fun RecyclerView.removeModel(
    m: Any,
    position: Int,
) {
    mutable.remove(m)
    adapter?.notifyItemRemoved(position)
}

fun RecyclerView.removeModel(position: Int) {
    mutable.removeAt(position)
    adapter?.notifyItemRemoved(position)
}
