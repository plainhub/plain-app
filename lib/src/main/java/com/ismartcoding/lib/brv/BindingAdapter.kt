@file:Suppress("PropertyName")

package com.ismartcoding.lib.brv

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.NoSuchPropertyException
import android.view.*
import androidx.annotation.IdRes
import androidx.annotation.IntRange
import androidx.annotation.LayoutRes
import androidx.appcompat.widget.PopupMenu
import androidx.databinding.DataBindingUtil
import androidx.databinding.ViewDataBinding
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.NO_ID
import androidx.viewbinding.ViewBinding
import com.ismartcoding.lib.brv.animation.*
import com.ismartcoding.lib.brv.annotaion.AnimationType
import com.ismartcoding.lib.brv.item.*
import com.ismartcoding.lib.brv.listener.*
import com.ismartcoding.lib.brv.listener.ProxyDiffCallback
import com.ismartcoding.lib.brv.utils.BRV
import com.ismartcoding.lib.brv.utils.models
import com.ismartcoding.lib.brv.utils.setDifferModels
import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Modifier
import java.util.concurrent.*
import kotlin.math.min

@Suppress("UNCHECKED_CAST", "MemberVisibilityCanBePrivate")
open class BindingAdapter : RecyclerView.Adapter<BindingAdapter.BindingViewHolder>() {
    /** 当前Adapter被setAdapter才不为null */
    var rv: RecyclerView? = null

    /** onBindViewHolder触发监听器集合 */
    var onBindViewHolders = mutableListOf<OnBindViewHolderListener>()

    /**
     * 单独配置modelId, 会忽略[BRV.modelId]
     * @see BRV.modelId
     */
    var modelId: Int = BRV.modelId

    companion object {
        private val dataBindingEnable: Boolean by lazy {
            try {
                Class.forName("androidx.databinding.DataBindingUtil")
                true
            } catch (e: Throwable) {
                false
            }
        }
    }

    // <editor-fold desc="生命周期">
    private var onCreate: (BindingViewHolder.(viewType: Int) -> Unit)? = null
    private var onBind: (BindingViewHolder.() -> Unit)? = null
    private var onPayload: (BindingViewHolder.(payloads: MutableList<Any>) -> Unit)? = null
    private var onClick: (BindingViewHolder.(viewId: Int) -> Unit)? = null
    private var onLongClick: (BindingViewHolder.(viewId: Int) -> Unit)? = null
    private var onChecked: ((position: Int, checked: Boolean, allChecked: Boolean) -> Unit)? = null
    private var onToggle: ((position: Int, toggleMode: Boolean, end: Boolean) -> Unit)? = null

    /**
     * [onCreateViewHolder]执行时回调
     */
    fun onCreate(block: BindingViewHolder.(viewType: Int) -> Unit) {
        onCreate = block
    }

    /**
     * [onBindViewHolder]执行时回调
     */
    fun onBind(block: BindingViewHolder.() -> Unit) {
        onBind = block
    }

    /**
     * 增量更新回调
     * 当你使用[notifyItemChanged(int, Object)]或者[notifyItemRangeChanged(int, Object)]等方法更新列表时才会触发, 并且形参payload要求不能为null
     *
     * @param block 多次调用[notifyItemChanged]会将payload合并到一个集合中payloads
     */
    fun onPayload(block: BindingViewHolder.(payloads: MutableList<Any>) -> Unit) {
        onPayload = block
    }

    override fun getItemId(position: Int): Long {
        return getModelOrNull<ItemStableId>(position)?.getItemId() ?: NO_ID
    }

    // </editor-fold>

    // <editor-fold desc="覆写函数">

    private var context: Context? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): BindingViewHolder {
        val vh =
            if (dataBindingEnable) {
                val viewBinding =
                    DataBindingUtil.inflate<ViewDataBinding>(
                        LayoutInflater.from(parent.context),
                        viewType,
                        parent,
                        false,
                    )
                if (viewBinding == null) {
                    BindingViewHolder(parent.getView(viewType))
                } else {
                    BindingViewHolder(
                        viewBinding,
                    )
                }
            } else {
                BindingViewHolder(parent.getView(viewType))
            }
        onCreate?.invoke(vh, viewType)
        return vh
    }

    fun ViewGroup.getView(
        @LayoutRes layout: Int,
    ): View {
        return LayoutInflater.from(context).inflate(layout, this, false)
    }

    override fun onBindViewHolder(
        holder: BindingViewHolder,
        position: Int,
    ) {
        holder.bind(getModel(position))
    }

    override fun onBindViewHolder(
        holder: BindingViewHolder,
        position: Int,
        payloads: MutableList<Any>,
    ) {
        if (onPayload != null && payloads.isNotEmpty()) {
            onPayload?.invoke(holder, payloads)
        } else {
            super.onBindViewHolder(holder, position, payloads)
        }
    }

    override fun getItemViewType(position: Int): Int {
        val model = getModel<Any>(position)
        val modelClass: Class<*> = model.javaClass
        return (
            typePool[modelClass]?.invoke(model, position) ?: interfacePool?.run {
                for (interfaceType in this) {
                    if (interfaceType.key.isAssignableFrom(modelClass)) {
                        return@run interfaceType.value.invoke(model, position)
                    }
                }
                null
            }
                ?: throw NoSuchPropertyException("please add item model type : addType<${model.javaClass.name}>(R.layout.item)")
        )
    }

    override fun getItemCount(): Int {
        return headerCount + modelCount + footerCount
    }

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        this.rv = recyclerView
        if (context == null) {
            context = recyclerView.context
        }
        itemTouchHelper?.attachToRecyclerView(recyclerView)
    }

    override fun onViewAttachedToWindow(holder: BindingViewHolder) {
        val layoutPosition = holder.layoutPosition
        if (animationEnabled && lastPosition < layoutPosition) {
            itemAnimation.onItemEnterAnimation(holder.itemView)
            lastPosition = layoutPosition
        }
        holder.getModelOrNull<ItemAttached>()?.onViewAttachedToWindow(holder)
    }

    override fun onViewDetachedFromWindow(holder: BindingViewHolder) {
        holder.getModelOrNull<ItemAttached>()?.onViewDetachedFromWindow(holder)
    }

    // </editor-fold>

    // <editor-fold desc="多类型">

    /** 类型池 */
    val typePool = mutableMapOf<Class<*>, Any.(Int) -> Int>()
    var interfacePool: MutableMap<Class<*>, Any.(Int) -> Int>? = null

    /**
     * 添加多类型
     * 在BRV中一个Item类型就是对应一个唯一的布局文件Id, 而[M]即为对应该类型所需的数据类型. 只要使用该函数添加的元素类型才被允许赋值给[models].
     * 然后假设你使用DataBinding的话, 则该类型对应在[models]中的对象会被DataBinding绑定都对应的布局上. 前提是你有在xml中声明数据类型
     */
    inline fun <reified M> addType(
        @LayoutRes layout: Int,
    ) {
        if (Modifier.isInterface(M::class.java.modifiers)) {
            M::class.java.addInterfaceType { layout }
        } else {
            typePool[M::class.java] = { layout }
        }
    }

    /**
     * 通过回调函数添加多类型, 一对多多类型(即一个数据类对应多个布局)
     * [block]中的position为当前item位于列表中的索引, [M]则为rv的models中对应的数据类型
     */
    inline fun <reified M> addType(noinline block: M.(position: Int) -> Int) {
        if (Modifier.isInterface(M::class.java.modifiers)) {
            M::class.java.addInterfaceType(block as Any.(Int) -> Int)
        } else {
            typePool[M::class.java] = block as Any.(Int) -> Int
        }
    }

    /**
     * 接口类型, 即类型必须为接口, 同时其子类都会被认为属于该接口而对应其布局
     * @receiver 接口类
     * @see addType
     */
    fun Class<*>.addInterfaceType(block: Any.(Int) -> Int) {
        (
            interfacePool ?: mutableMapOf<Class<*>, Any.(Int) -> Int>().also {
                interfacePool = it
            }
        )[this] = block
    }
    // </editor-fold>

    // <editor-fold desc="触摸事件">
    private val clickListeners = HashMap<Int, Pair<(BindingViewHolder.(Int) -> Unit)?, Boolean>>()
    private val longClickListeners = HashMap<Int, (BindingViewHolder.(Int) -> Unit)?>()

    /** 自定义ItemTouchHelper即可设置该属性 */
    var itemTouchHelper: ItemTouchHelper? = ItemTouchHelper(DefaultItemTouchCallback())
        set(value) {
            if (value == null) field?.attachToRecyclerView(null) else value.attachToRecyclerView(rv)
            field = value
        }

    /** 防抖动点击事件的间隔时间, 单位毫秒 */
    var clickThrottle: Long = BRV.clickThrottle

    /**
     * 监听指定Id控件的点击事件, 包含防抖动
     */
    fun onClick(
        @IdRes vararg id: Int,
        block: BindingViewHolder.(viewId: Int) -> Unit,
    ) {
        for (i in id) {
            clickListeners[i] = Pair(block, false)
        }
        onClick = block
    }

    /**
     * 监听指定Id控件的点击事件
     */
    fun onFastClick(
        @IdRes vararg id: Int,
        block: BindingViewHolder.(viewId: Int) -> Unit,
    ) {
        for (i in id) {
            clickListeners[i] = Pair(block, true)
        }
        onClick = block
    }

    /**
     * 长按点击事件回调
     */
    fun onLongClick(
        @IdRes vararg id: Int,
        block: BindingViewHolder.(viewId: Int) -> Unit,
    ) {
        for (i in id) {
            longClickListeners[i] = block
        }
        onLongClick = block
    }

    /**
     * 添加点击事件, 点击启用防抖动
     */
    fun @receiver:IdRes Int.onClick(listener: BindingViewHolder.(viewId: Int) -> Unit) {
        clickListeners[this] = Pair(listener, false)
    }

    fun @receiver:IdRes Int.checkable() {
        this.onClick {
            checkedSwitch(bindingAdapterPosition)
        }
    }

    /**
     * 添加点击事件
     */
    fun @receiver:IdRes Int.onFastClick(listener: BindingViewHolder.(viewId: Int) -> Unit) {
        clickListeners[this] = Pair(listener, true)
    }

    /**
     * 添加长按事件
     */
    fun @receiver:IdRes Int.onLongClick(listener: BindingViewHolder.(viewId: Int) -> Unit) {
        longClickListeners[this] = listener
    }

    fun @receiver:IdRes Int.onPopupMenu(block: PopupMenu.(holder: BindingViewHolder) -> Unit) {
        longClickListeners[this] = label@{
            if (toggleMode) {
                return@label
            }
            val popup = PopupMenu(context, itemView)
            popup.gravity = Gravity.END
            block(popup, this)
            popup.show()
        }
    }

    // </editor-fold>

    // <editor-fold desc="列表动画">

    private var itemAnimation: ItemAnimation = AlphaItemAnimation()
    private var lastPosition = -1
    private var isFirst = true

    /** 是否启用条目动画 */
    var animationEnabled = false

    /**
     * 自定义条目的动画样式
     */
    fun setAnimation(itemAnimation: ItemAnimation) {
        this.animationEnabled = true
        this.itemAnimation = itemAnimation
    }

    /**
     * 设置当前库自带的条目的动画样式
     */
    fun setAnimation(animationType: AnimationType) {
        this.animationEnabled = true
        when (animationType) {
            AnimationType.ALPHA -> this.itemAnimation = AlphaItemAnimation()
            AnimationType.SCALE -> this.itemAnimation = ScaleItemAnimation()
            AnimationType.SLIDE_BOTTOM -> this.itemAnimation = SlideBottomItemAnimation()
            AnimationType.SLIDE_LEFT -> this.itemAnimation = SlideLeftItemAnimation()
            AnimationType.SLIDE_RIGHT -> this.itemAnimation = SlideRightItemAnimation()
        }
    }

    // </editor-fold>

    // <editor-fold desc="头布局">

    /** 头布局的数据模型 */
    var headers: List<Any?> = mutableListOf()
        set(value) {
            field = if (value is ArrayList) value else value.toMutableList()
            notifyDataSetChanged()
        }

    /** 头布局数量 */
    val headerCount: Int get() = headers.size

    /**
     * 添加头布局
     * @param model 数据
     * @param index 插入头布局中的索引
     * @param animation 是否显示动画
     */
    fun addHeader(
        model: Any?,
        @IntRange(from = -1) index: Int = -1,
        animation: Boolean = false,
    ) {
        if (index == -1) {
            (headers as MutableList).add(0, model)
            if (animation) notifyItemInserted(0)
        } else if (index <= headerCount) {
            (headers as MutableList).add(index, model)
            if (animation) notifyItemInserted(index)
        }

        if (!animation) notifyDataSetChanged()
    }

    /**
     * 通过数据删除头布局
     */
    fun removeHeader(
        model: Any?,
        animation: Boolean = false,
    ) {
        if (headerCount != 0 && headers.contains(model)) {
            val headerIndex = headers.indexOf(model)

            (headers as MutableList).remove(model)
            if (animation) {
                notifyItemRemoved(headerIndex)
            } else {
                notifyDataSetChanged()
            }
        }
    }

    /**
     * 通过索引删除头布局
     */
    fun removeHeaderAt(
        @IntRange(from = 0) index: Int = 0,
        animation: Boolean = false,
    ) {
        if (headerCount <= 0 || headerCount < index) return
        (headers as MutableList).removeAt(index)
        if (animation) notifyItemRemoved(index) else notifyDataSetChanged()
    }

    fun clearHeader(animation: Boolean = false) {
        if (headers.isNotEmpty()) {
            val headerCount = this.headerCount
            (headers as MutableList).clear()
            if (animation) notifyItemRangeRemoved(0, headerCount) else notifyDataSetChanged()
        }
    }

    /**
     * 指定position是否为头布局
     */
    fun isHeader(
        @IntRange(from = 0) position: Int,
    ): Boolean = (headerCount > 0 && position < headerCount)

    // </editor-fold>

    // <editor-fold desc="脚布局">

    /**
     * 全部脚布局数据集合
     */
    var footers: List<Any?> = mutableListOf()
        set(value) {

            field = if (value is ArrayList) value else value.toMutableList()
            notifyDataSetChanged()

            if (isFirst) {
                lastPosition = -1
                isFirst = false
            } else {
                lastPosition = itemCount - 1
            }
        }

    /**
     * 脚布局数量
     */
    val footerCount: Int get() = footers.size

    /**
     * 添加脚布局
     * @param model 数据模型
     * @param index 指定插入的脚布局索引
     * @param animation 是否显示动画
     */
    fun addFooter(
        model: Any?,
        @IntRange(from = -1) index: Int = -1,
        animation: Boolean = false,
    ) {
        if (index == -1) {
            (footers as MutableList).add(model)
            if (animation) {
                notifyItemInserted(itemCount)
            }
        } else if (index <= footerCount) {
            (footers as MutableList).add(index, model)
            if (animation) {
                notifyItemInserted(headerCount + modelCount + index)
            }
        }

        if (!animation) {
            notifyDataSetChanged()
        }
    }

    /**
     * 通过数据模型删除脚布局
     */
    fun removeFooter(
        model: Any?,
        animation: Boolean = false,
    ) {
        if (footerCount != 0 && footers.contains(model)) {
            val footerIndex = headerCount + modelCount + footers.indexOf(model)
            (footers as MutableList).remove(model)
            if (animation) {
                notifyItemRemoved(footerIndex)
            } else {
                notifyDataSetChanged()
            }
        }
    }

    /**
     * 通过索引删除脚布局
     */
    fun removeFooterAt(
        @IntRange(from = -1) index: Int = -1,
        animation: Boolean = false,
    ) {
        if (footerCount <= 0 || footerCount < index) return

        if (index == -1) {
            (footers as MutableList).removeAt(0)
            if (animation) {
                notifyItemRemoved(headerCount + modelCount)
            }
        } else {
            (footers as MutableList).removeAt(index)

            if (animation) {
                notifyItemRemoved(headerCount + modelCount + index)
            }
        }

        if (!animation) {
            notifyDataSetChanged()
        }
    }

    /**
     * 清除全部脚布局
     */
    fun clearFooter(animation: Boolean = false) {
        if (footers.isNotEmpty()) {
            val footerCount = this.footerCount
            (footers as MutableList).clear()
            if (animation) {
                notifyItemRangeRemoved(headerCount + modelCount, itemCount + footerCount)
            } else {
                notifyDataSetChanged()
            }
        }
    }

    /**
     * 指定position是否为脚布局
     */
    fun isFooter(
        @IntRange(from = 0) position: Int,
    ): Boolean = (footerCount > 0 && position >= headerCount + modelCount && position < itemCount)

    // </editor-fold>

    // <editor-fold desc="数据">

    /** 数据模型数量(不包含头布局和脚布局) */
    val modelCount: Int
        get() {
            return if (models == null) 0 else models!!.size
        }

    /** 原始的数据集合对象, 不会经过任何处理 */
    var _data: MutableList<Any>? = null

    /**
     * 数据模型集合
     * 如果赋值的是[List]不可变集合将会自动被替换成[MutableList], 将无法保持为同一个集合对象引用
     */
    var models: List<Any>?
        get() = _data

        @SuppressLint("NotifyDataSetChanged")
        set(value) {
            _data =
                when (value) {
                    is ArrayList -> flat(value)
                    is List -> flat(value.toMutableList())
                    else -> null
                }
            notifyDataSetChanged()
            checkedPosition.clear()
            if (isFirst) {
                lastPosition = -1
                isFirst = false
            } else {
                lastPosition = itemCount - 1
            }
        }

    @Suppress("UNCHECKED_CAST")
    fun <M> getModelList(): List<M> {
        return (models as? List<M>) ?: arrayListOf()
    }

    /** 对比新旧数据更改列表接口 */
    var itemDifferCallback: ItemDifferCallback = ItemDifferCallback

    /**
     * 对比数据, 根据数据差异自动刷新列表
     * 数据对比默认使用`equals`函数对比, 你可以为数据手动实现equals函数来修改对比逻辑. 推荐定义数据为 data class, 因其会根据构造参数自动生成equals
     * 如果数据集合很大导致对比速度很慢, 建议在非主步线程中调用此函数, 效果等同于[androidx.recyclerview.widget.AsyncListDiffer]
     *
     * 对于数据是否匹配可能需要你自定义[itemDifferCallback], 因为默认使用数据模型的[equals]方法匹配, 具体请阅读[ItemDifferCallback.DEFAULT]
     *
     * @param newModels 新的数据, 将覆盖旧的数据
     * @param detectMoves 是否对比Item的移动, true会导致列表当前位置发生移动
     * @param commitCallback 因为子线程调用[setDifferModels]刷新列表会不同步(刷新列表需要切换到主线程), 而[commitCallback]保证在刷新列表完成以后调用(运行在主线程)
     */
    fun setDifferModels(
        newModels: List<Any>?,
        detectMoves: Boolean = true,
        commitCallback: Runnable? = null,
    ) {
        val oldModels = _data
        _data =
            when (newModels) {
                is ArrayList -> flat(newModels)
                is List -> flat(newModels.toMutableList())
                else -> null
            }
        val diffResult = DiffUtil.calculateDiff(ProxyDiffCallback(newModels, oldModels, itemDifferCallback), detectMoves)
        val mainLooper = Looper.getMainLooper()
        if (Looper.myLooper() != mainLooper) {
            Handler(mainLooper).post {
                diffResult.dispatchUpdatesTo(this)
                commitCallback?.run()
            }
        } else {
            diffResult.dispatchUpdatesTo(this)
            commitCallback?.run()
        }
        checkedPosition.clear()
        if (isFirst) {
            lastPosition = -1
            isFirst = false
        } else {
            lastPosition = itemCount - 1
        }
    }

    /** 可增删的数据模型集合, 本质上就是返回可变的models. 假设未赋值给models则将抛出异常为[ClassCastException] */
    var mutable
        get() = models as ArrayList
        set(value) {
            models = value
        }

    /**
     * 扁平化数据, 将折叠分组铺平展开创建列表
     * @param models 数据集合
     * @param expand 是否展开或折叠其子分组, null则什么都不做
     * @param depth 扁平化深度层级 -1 表示全部
     */
    private fun flat(
        models: MutableList<Any>,
        expand: Boolean? = null,
        @IntRange(from = -1) depth: Int = 0,
    ): MutableList<Any> {
        if (models.isEmpty()) return models
        val arrayList = ArrayList(models)
        models.clear()

        arrayList.forEachIndexed { index, item ->
            models.add(item)
            if (item is ItemExpand) {
                item.itemGroupPosition = index
                var nextDepth = depth
                if (expand != null && depth != 0) {
                    item.itemExpand = expand
                    if (depth > 0) nextDepth -= 1
                }

                val itemSublist = item.itemSublist
                if (!itemSublist.isEmpty() && (item.itemExpand || (depth != 0 && expand != null))) {
                    val nestedList = flat(ArrayList(itemSublist), expand, nextDepth)
                    models.addAll(nestedList)
                }
            }
        }
        return models
    }

    /**
     * 指定position是否为models
     */
    fun isModel(
        @IntRange(from = 0) position: Int,
    ): Boolean = !(isHeader(position) || isFooter(position))

    /**
     * 根据索引返回数据模型, 如果不存在该模型则返回Null
     */
    inline fun <reified M> getModelOrNull(position: Int): M? {
        return when {
            isHeader(position) -> headers[position] as? M
            isFooter(position) -> footers[position - headerCount - modelCount] as? M
            else -> models?.let { it.getOrNull(position - headerCount) as? M }
        }
    }

    /**
     * 根据索引返回数据模型, 不存在该模型则抛出异常
     */
    fun <M> getModel(
        @IntRange(from = 0) position: Int,
    ): M {
        return when {
            isHeader(position) -> headers[position] as M
            isFooter(position) -> footers[position - headerCount - modelCount] as M
            else -> models!!.let { it[position - headerCount] as M }
        }
    }

    /**
     * 添加新的数据
     * @param models 被添加的数据
     * @param animation 是否使用动画
     * @param index 插入到[models]指定位置, 如果index超过[models]长度则会添加到最后
     */
    @SuppressLint("NotifyDataSetChanged")
    fun addModels(
        models: List<Any>?,
        animation: Boolean = true,
        @IntRange(from = -1) index: Int = -1,
    ) {
        if (models.isNullOrEmpty()) return
        val data: MutableList<Any> =
            when (models) {
                is ArrayList -> models
                else -> models.toMutableList()
            }
        when {
            this.models == null -> {
                this.models = flat(data)
                notifyDataSetChanged()
            }
            this.models?.isEmpty() == true -> {
                (this.models as? MutableList)?.let {
                    it.addAll(flat(data))
                    notifyDataSetChanged()
                }
            }
            else -> {
                val realModels = this.models as MutableList
                var insertIndex: Int = headerCount
                if (index == -1 || realModels.size < index) {
                    insertIndex += realModels.size
                    realModels.addAll(flat(data))
                } else {
                    if (checkedPosition.isNotEmpty()) {
                        val insertSize = models.size
                        val iterator = checkedPosition.listIterator()
                        while (iterator.hasNext()) {
                            iterator.set(iterator.next() + insertSize)
                        }
                    }
                    insertIndex += index
                    realModels.addAll(index, flat(data))
                }
                if (animation) {
                    notifyItemRangeInserted(insertIndex, data.size)
                    rv?.post {
                        rv?.invalidateItemDecorations()
                    }
                } else {
                    notifyDataSetChanged()
                }
            }
        }
    }

    /**
     * 对应models中的index
     */
    fun Int.toModelPosition(): Int = this - headerCount

    // </editor-fold>

    // <editor-fold desc="切换模式">

    /** 是否开启切换模式 */
    var toggleMode = false
        private set

    /**
     * 切换模式, 切换模式为遍历每个item
     * @see [onToggle]
     */
    fun toggle() {
        onToggle?.let {
            toggleMode = !toggleMode
            for (position in 0 until itemCount) {
                if (position != itemCount - 1) {
                    it.invoke(position, toggleMode, false)
                } else {
                    it.invoke(position, toggleMode, true)
                }
            }
        }
    }

    /**
     * 设置切换模式, 切换模式为遍历每个item
     * @see [onToggle]
     * @param toggleMode Boolean
     */
    fun toggle(toggleMode: Boolean) {
        if (toggleMode != this.toggleMode) toggle()
    }

    /**
     * 切换模式事件回调
     *
     * @param position 条目位置
     * @param toggleMode 切换模式
     * @param end 全部条目已经切换完成
     */
    fun onToggle(block: (position: Int, toggleModel: Boolean, end: Boolean) -> Unit) {
        onToggle = block
    }
    // </editor-fold>

    // <editor-fold desc="选择模式">

    private var checkableItemTypeList: List<Int>? = null

    /** 已选择条目的position */
    val checkedPosition = mutableListOf<Int>()

    fun fixCheckedPosition(
        position: Int,
        isChecked: Boolean,
    ) {
        if (isChecked) {
            if (!checkedPosition.contains(position)) {
                checkedPosition.add(position)
            }
        } else {
            if (checkedPosition.contains(position)) {
                checkedPosition.remove(position)
            }
        }
    }

    /** 已选择条目数量 */
    val checkedCount: Int get() = checkedPosition.size

    private val checkableCount: Int
        get() {
            var count = 0
            if (checkableItemTypeList == null) {
                return models!!.size
            } else {
                for (i in 0 until itemCount) {
                    if (checkableItemTypeList!!.contains(getItemViewType(i))) {
                        count++
                    }
                }
            }
            return count
        }

    /** 全局单选模式 */
    var singleMode = false
        set(value) {
            field = value
            val size = checkedPosition.size
            if (field && size > 1) {
                for (i in 0 until size - 1) setChecked(checkedPosition[0], false)
            }
        }

    /**
     * 返回被选中的条目对应的数据模型集合
     */
    inline fun <reified M> getCheckedModels(): List<M> {
        val checkedModels = ArrayList<M>()
        for (position in this.checkedPosition) {
            val m = getModelOrNull(position) as? M
            if (m != null) {
                checkedModels.add(m)
            }
        }
        return checkedModels
    }

    /**
     * 设置可以被选择的item类型
     * @see setChecked
     */
    fun setCheckableType(
        @LayoutRes vararg checkableItemType: Int,
    ) {
        checkableItemTypeList = checkableItemType.toMutableList()
    }

    /**
     * 单选模式下不支持全选, 但支持取消全部选择
     * @param checked true为全选, false 取消全部选择
     */
    fun checkedAll(checked: Boolean = true) {
        if (checked) {
            if (singleMode) return
            for (i in 0 until itemCount) {
                if (!checkedPosition.contains(i)) {
                    setChecked(i, true)
                }
            }
        } else {
            for (i in 0 until itemCount) {
                if (checkedPosition.contains(i)) setChecked(i, false)
            }
        }
    }

    /**
     * 是否全选
     */
    fun isCheckedAll(): Boolean = checkedCount == checkableCount

    /**
     * 反选
     */
    fun checkedReverse() {
        if (singleMode) return
        for (i in 0 until itemCount) {
            if (checkedPosition.contains(i)) {
                setChecked(i, false)
            } else {
                setChecked(i, true)
            }
        }
    }

    /**
     * 设置选中
     */
    fun setChecked(
        @IntRange(from = 0) position: Int,
        checked: Boolean,
    ) {
        if ((checkedPosition.contains(position) && checked) ||
            (!checked && !checkedPosition.contains(position))
        ) {
            return
        }

        val itemViewType = getItemViewType(position)

        if (checkableItemTypeList?.contains(itemViewType) == false) return

        if (onChecked == null) return

        if (checked) {
            checkedPosition.add(position)
        } else {
            checkedPosition.remove(Integer.valueOf(position))
        }

        if (singleMode && checked && checkedPosition.size > 1) {
            setChecked(checkedPosition[0], false)
        }
        onChecked?.invoke(position, checked, isCheckedAll())
    }

    /**
     * 切换选中
     */
    fun checkedSwitch(
        @IntRange(from = 0) position: Int,
    ) {
        if (checkedPosition.contains(position)) {
            setChecked(position, false)
        } else {
            setChecked(position, true)
        }
    }

    /**
     * 条目选中事件回调
     */
    fun onChecked(block: (position: Int, checked: Boolean, allChecked: Boolean) -> Unit) {
        onChecked = block
    }

    // </editor-fold>

    // <editor-fold desc="分组">

    private var previousExpandPosition = -1
    private var onExpand: (BindingViewHolder.(Boolean) -> Unit)? = null

    /** 分组展开和折叠是否启用动画 */
    var expandAnimationEnabled = true

    /** 只允许一个条目展开(展开当前条目就会折叠上个条目) */
    var singleExpandMode = false

    /** 监听展开分组 */
    fun onExpand(block: BindingViewHolder.(Boolean) -> Unit) {
        this.onExpand = block
    }

    /**
     * 判断两个位置的item是否属于同一分组下, 要求这两个位置的item都展开才有效
     * 如果其中一个item都属于根节点则返回-1, 这种情况不算属于同一分组下
     */
    fun isSameGroup(
        @IntRange(from = 0) position: Int,
        @IntRange(from = 0) otherPosition: Int,
    ): Boolean {
        val aModel = models?.getOrNull(otherPosition) ?: return false
        val bModel = models?.getOrNull(otherPosition) ?: return false
        for (index in min(position, otherPosition) - 1 downTo 0) {
            val item = models?.getOrNull(index) ?: break
            if (item is ItemExpand && item.itemSublist.contains(aModel) && item.itemSublist.contains(bModel)
            ) {
                return true
            }
        }
        return false
    }

    /**
     * 展开
     * @param position 指定position的条目折叠
     * @param scrollTop 展开同时当前条目滑动到顶部
     * @param depth 递归展开子项的深度, 如等于-1则代表展开所有子项, 0表示仅展开当前
     * @return 展开后增加的条目数量
     */
    fun expand(
        @IntRange(from = 0) position: Int,
        scrollTop: Boolean = false,
        @IntRange(from = -1) depth: Int = 0,
    ): Int {
        val holder =
            rv?.findViewHolderForLayoutPosition(position) as? BindingViewHolder ?: rv?.run {
                val holder = createViewHolder(this, getItemViewType(position))
                bindViewHolder(holder, position)
                holder
            } ?: return 0
        return holder.expand(scrollTop, depth)
    }

    /**
     * 折叠
     * @param position 指定position的条目折叠
     * @param depth 递归展开子项的深度, 如等于-1则代表展开所有子项, 0表示仅展开当前
     * @return 折叠后消失的条目数量
     */
    fun collapse(
        @IntRange(from = 0) position: Int,
        @IntRange(from = -1) depth: Int = 0,
    ): Int {
        val holder =
            rv?.findViewHolderForLayoutPosition(position) as? BindingViewHolder ?: rv?.run {
                val holder = createViewHolder(this, getItemViewType(position))
                bindViewHolder(holder, position)
                holder
            } ?: return 0
        return holder.collapse(depth)
    }

    /**
     * 展开或折叠
     * @param scrollTop 展开同时当前条目滑动到顶部
     * @param depth 递归展开子项的深度, 如等于-1则代表展开所有子项, 0表示仅展开当前
     * @return 展开或折叠后变动的条目数量
     */
    fun expandOrCollapse(
        @IntRange(from = 0) position: Int,
        scrollTop: Boolean = false,
        @IntRange(from = -1) depth: Int = 0,
    ): Int {
        val holder =
            rv?.findViewHolderForLayoutPosition(position) as? BindingViewHolder ?: rv?.run {
                val holder = createViewHolder(this, getItemViewType(position))
                bindViewHolder(holder, position)
                holder
            } ?: return 0
        return holder.expandOrCollapse(scrollTop, depth)
    }

    // </editor-fold>

    inner class BindingViewHolder : RecyclerView.ViewHolder {
        lateinit var _data: Any private set
        var context: Context = this@BindingAdapter.context!!
        val adapter: BindingAdapter = this@BindingAdapter
        val modelPosition get() = layoutPosition - headerCount

        @PublishedApi
        internal var viewBinding: ViewBinding? = null

        constructor(itemView: View) : super(itemView)

        constructor(viewBinding: ViewDataBinding) : super(viewBinding.root) {
            this.viewBinding = viewBinding
        }

        init {
            for (clickListener in clickListeners) {
                val view = itemView.findViewById<View>(clickListener.key) ?: continue
                if (clickListener.value.second) {
                    view.setOnClickListener {
                        (clickListener.value.first ?: onClick)?.invoke(this, it.id)
                    }
                } else {
                    view.throttleClick(clickThrottle) {
                        (clickListener.value.first ?: onClick)?.invoke(this@BindingViewHolder, id)
                    }
                }
            }
            for (longClickListener in longClickListeners) {
                val view = itemView.findViewById<View>(longClickListener.key) ?: continue
                view.setOnLongClickListener {
                    (longClickListener.value ?: onLongClick)?.invoke(this, it.id)
                    true
                }
            }
        }

        internal fun bind(model: Any) {
            this._data = model

            onBindViewHolders.forEach {
                it.onBindViewHolder(rv!!, adapter, this, bindingAdapterPosition)
            }

            if (model is ItemPosition) {
                model.itemPosition = modelPosition
            }

            if (model is ItemBind) {
                model.onBind(this)
            }

            onBind?.invoke(this@BindingViewHolder)

            val viewBinding = viewBinding
            if (dataBindingEnable && viewBinding is ViewDataBinding) {
                try {
                    viewBinding.setVariable(modelId, model)
                    viewBinding.executePendingBindings()
                } catch (e: Exception) {
                    val message =
                        "DataBinding type mismatch ...(${context.resources.getResourceEntryName(itemViewType)}.xml:1)"
                    Log.e(javaClass.simpleName, message, e)
                }
            }
        }

        /**
         * 返回匹配泛型的数据绑定对象ViewDataBinding
         */
        inline fun <reified B : ViewBinding> getBinding(): B {
            return if (viewBinding == null) {
                val method = B::class.java.getMethod("bind", View::class.java)
                val viewBinding = method.invoke(null, itemView) as B
                this.viewBinding = viewBinding
                viewBinding
            } else {
                viewBinding as B
            }
        }

        inline fun <reified B : ViewBinding> getBindingOrNull(): B? {
            return if (viewBinding == null) {
                try {
                    val method = B::class.java.getMethod("bind", View::class.java)
                    val viewBinding = method.invoke(null, itemView) as? B
                    this.viewBinding = viewBinding
                    viewBinding
                } catch (e: InvocationTargetException) {
                    null
                }
            } else {
                viewBinding as? B
            }
        }

        /**
         * 查找ItemView上的视图
         */
        fun <V : View?> findView(
            @IdRes id: Int,
        ): V = itemView.findViewById(id)

        /**
         * 返回数据模型
         */
        fun <M> getModel(): M = _data as M

        /**
         * 返回数据模型, 如果不匹配泛型则返回Null
         */
        inline fun <reified M> getModelOrNull(): M? = _data as? M

        // <editor-fold desc="分组">

        /**
         * 展开子项
         * @param scrollTop 展开同时当前条目滑动到顶部
         * @param depth 递归展开子项的深度, 如等于-1则代表展开所有子项, 0表示仅展开当前
         * @return 展开后新增的条目数量
         */
        fun expand(
            scrollTop: Boolean = false,
            @IntRange(from = -1) depth: Int = 0,
        ): Int {
            val itemExpand = getModelOrNull<ItemExpand>()
            if (itemExpand?.itemExpand == true) return 0

            var position = if (bindingAdapterPosition == -1) layoutPosition else bindingAdapterPosition

            if (singleExpandMode && previousExpandPosition != -1 && findParentPosition() != previousExpandPosition) {
                val collapseCount = adapter.collapse(previousExpandPosition)
                if (position > previousExpandPosition) {
                    position -= collapseCount
                }
            }

            onExpand?.invoke(this, true)

            return if (itemExpand != null && !itemExpand.itemExpand) {
                val itemSublist = itemExpand.itemSublist
                itemExpand.itemExpand = true
                previousExpandPosition = position
                if (itemSublist.isNullOrEmpty()) {
                    notifyItemChanged(position)
                    0
                } else {
                    val sublistFlat = flat(ArrayList(itemSublist), true, depth)

                    (this@BindingAdapter.models as MutableList).addAll(position + 1 - headerCount, sublistFlat)
                    if (expandAnimationEnabled) {
                        notifyItemChanged(position)
                        notifyItemRangeInserted(position + 1, sublistFlat.size)
                    } else {
                        notifyDataSetChanged()
                    }
                    if (scrollTop) {
                        rv?.let {
                            it.scrollToPosition(position)
                            (it.layoutManager as? LinearLayoutManager)?.scrollToPositionWithOffset(position, 0)
                        }
                    }
                    sublistFlat.size
                }
            } else {
                0
            }
        }

        /**
         * 折叠子项
         * @param depth 递归折叠子项的深度, 如等于-1则代表展开所有子项, 0表示仅展开当前
         * @return 折叠后减少的条目数量
         */
        fun collapse(
            @IntRange(from = -1) depth: Int = 0,
        ): Int {
            val itemExpand = getModelOrNull<ItemExpand>()

            if (itemExpand?.itemExpand == false) return 0
            val position = if (bindingAdapterPosition == -1) layoutPosition else bindingAdapterPosition

            onExpand?.invoke(this, false)

            return if (itemExpand != null && itemExpand.itemExpand) {
                val itemSublist = itemExpand.itemSublist
                itemExpand.itemExpand = false

                if (itemSublist.isNullOrEmpty()) {
                    notifyItemChanged(position, itemExpand)
                    0
                } else {
                    val sublistFlat = flat(ArrayList(itemSublist), false, depth)
                    (this@BindingAdapter.models as MutableList).subList(
                        position + 1 - headerCount,
                        position + 1 - headerCount + sublistFlat.size,
                    ).clear()
                    if (expandAnimationEnabled) {
                        notifyItemChanged(position, itemExpand)
                        notifyItemRangeRemoved(position + 1, sublistFlat.size)
                    } else {
                        notifyDataSetChanged()
                    }
                    sublistFlat.size
                }
            } else {
                0
            }
        }

        /**
         * 展开或折叠子项
         * @param scrollTop 展开同时当前条目滑动到顶部
         * @param depth 递归展开子项的深度, 如等于-1则代表展开所有子项, 0表示仅展开当前
         * @return 展开|折叠后变动的条目数量
         */
        fun expandOrCollapse(
            scrollTop: Boolean = false,
            @IntRange(from = -1) depth: Int = 0,
        ): Int {
            val itemExpand = getModelOrNull<ItemExpand>()
            return if (itemExpand != null) {
                if (itemExpand.itemExpand) collapse(depth) else expand(scrollTop, depth)
            } else {
                0
            }
        }

        /**
         * 查找分组中的父项位置
         * @return -1 表示不存在父项
         */
        fun findParentPosition(): Int {
            for (index in layoutPosition - 1 downTo 0) {
                val item = models?.getOrNull(index) ?: break
                if (item is ItemExpand && item.itemSublist.contains(_data)) {
                    return index
                }
            }
            return -1
        }

        /**
         * 查找分组中的父项ViewHolder
         * @return null表示不存在父项或没有显示在屏幕中
         */
        fun findParentViewHolder(): BindingViewHolder? {
            return rv?.findViewHolderForLayoutPosition(findParentPosition()) as? BindingViewHolder
        }

        // </editor-fold>
    }

    // <editor-fold desc="悬停">

    /**
     * 是否启用条目悬停
     */
    var hoverEnabled = true

    /**
     * 监听开始悬停
     */
    var onHoverAttachListener: OnHoverAttachListener? = null

    /**
     * 通过position判断是否启用悬停
     */
    fun isHover(position: Int): Boolean {
        val model = getModelOrNull<ItemHover>(position)
        return model != null && model.itemHover && hoverEnabled
    }
    // </editor-fold>
}
