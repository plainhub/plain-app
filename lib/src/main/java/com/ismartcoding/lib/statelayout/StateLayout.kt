package com.ismartcoding.lib.statelayout

import android.content.Context
import android.content.res.Resources
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.ArrayMap
import android.util.AttributeSet
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.annotation.IdRes
import androidx.annotation.LayoutRes
import com.ismartcoding.lib.R
import com.ismartcoding.lib.runMain
import com.ismartcoding.lib.statelayout.StateConfig.isNetworkingRetry
import com.ismartcoding.lib.statelayout.StateConfig.setRetryIds
import com.ismartcoding.lib.statelayout.Status.*

/**
 * 应用全局缺省页
 *
 * 全局配置
 * 单例配置
 * 局部缺省页
 * 数据传递
 * 监听缺省页显示生命周期
 * 自定义动画
 * 布局或代码声明皆可
 * 快速配置点击重试
 * 无网络立即显示错误缺省页
 */
class StateLayout
    @JvmOverloads
    constructor(
        context: Context,
        attrs: AttributeSet? = null,
        defStyleAttr: Int = 0,
    ) : FrameLayout(context, attrs, defStyleAttr) {
        private val statusContainer = ArrayMap<Status, StatusInfo>()
        private var stateChanged = false
        private var trigger = false

        private var retryIds: IntArray? = null
            get() = field ?: StateConfig.retryIds
        private var onEmpty: (View.(tag: Any?) -> Unit)? = null
            get() = field ?: StateConfig.onEmpty
        private var onError: (View.(tag: Any?) -> Unit)? = null
            get() = field ?: StateConfig.onError
        private var onContent: (View.(tag: Any?) -> Unit)? = null
            get() = field ?: StateConfig.onContent
        private var onLoading: (View.(tag: Any?) -> Unit)? = null
            get() = field ?: StateConfig.onLoading
        private var onRefresh: (StateLayout.(tag: Any?) -> Unit)? = null

        /** 当前缺省页是否加载成功过, 即是否执行过[showContent]*/
        var loaded = false
        var canRetry = true

        /**
         * 设置[setRetryIds]点击重试要求网络可用才会显示加载缺省,
         * 会回调[StateLayout.onRefresh]但不会回调[StateLayout.onLoading]
         * 为避免无网络情况下点击重试导致闪屏
         */
        var isNetworkingRetry = StateConfig.isNetworkingRetry

        /** 当前缺省页状态[Status] */
        var status = CONTENT
            private set

        // <editor-fold desc="设置缺省页">

        /** 错误页面布局 */
        @LayoutRes
        var errorLayout: Int = NO_ID
            get() = if (field == NO_ID) StateConfig.errorLayout else field
            set(value) {
                if (field != value) {
                    removeStatus(ERROR)
                    field = value
                }
            }

        /** 空页面布局 */
        @LayoutRes
        var emptyLayout: Int = NO_ID
            get() = if (field == NO_ID) StateConfig.emptyLayout else field
            set(value) {
                if (field != value) {
                    removeStatus(EMPTY)
                    field = value
                }
            }

        /** 加载中页面布局 */
        @LayoutRes
        var loadingLayout: Int = NO_ID
            get() = if (field == NO_ID) StateConfig.loadingLayout else field
            set(value) {
                if (field != value) {
                    removeStatus(LOADING)
                    field = value
                }
            }

        /** 处理缺省页状态变更 */
        var stateChangedHandler: StateChangedHandler? = null
            get() {
                return field ?: StateConfig.stateChangedHandler ?: StateChangedHandler
            }

        // </editor-fold>

        init {
            val attributes = context.obtainStyledAttributes(attrs, R.styleable.StateLayout)
            try {
                emptyLayout = attributes.getResourceId(R.styleable.StateLayout_empty_layout, NO_ID)
                errorLayout = attributes.getResourceId(R.styleable.StateLayout_error_layout, NO_ID)
                loadingLayout = attributes.getResourceId(R.styleable.StateLayout_loading_layout, NO_ID)
            } finally {
                attributes.recycle()
            }
        }

        override fun onFinishInflate() {
            super.onFinishInflate()
            if (childCount > 1 || childCount == 0) {
                throw UnsupportedOperationException("StateLayout only have one child view")
            }
            if (statusContainer.size == 0) {
                val view = getChildAt(0)
                setContent(view)
            }
        }

        // <editor-fold desc="监听缺省页">

        /**
         * 当加载中缺省页显示时回调
         * @see showLoading
         * @see StateConfig.onLoading
         */
        fun onLoading(block: View.(tag: Any?) -> Unit) =
            apply {
                onLoading = block
            }

        /**
         * 当空缺省页显示时回调
         * @see showEmpty
         * @see StateConfig.onEmpty
         */
        fun onEmpty(block: View.(tag: Any?) -> Unit) =
            apply {
                onEmpty = block
            }

        /**
         * 当错误缺省页显示时回调
         * @see showError
         * @see StateConfig.onError
         */
        fun onError(block: View.(tag: Any?) -> Unit) =
            apply {
                onError = block
            }

        /**
         * 当[showLoading]时会回调该函数参数, 一般将网络请求等异步操作放入其中
         */
        fun onRefresh(block: StateLayout.(tag: Any?) -> Unit) =
            apply {
                onRefresh = block
            }

        /**
         * 当[showContent]时会回调该函数参数, 一般将网络请求等异步操作放入其中
         * @see showContent
         * @see StateConfig.onContent
         */
        fun onContent(block: View.(tag: Any?) -> Unit) =
            apply {
                onContent = block
            }

        // </editor-fold>

        // <editor-fold desc="显示缺省页">

        /**
         * 默认情况下每次执行都会反复回调[onLoading](有网情况)
         * 但[onRefresh]不要求有网络
         *
         * @param tag 传递任意对象给[onLoading]函数
         * @param silent 仅执行[onRefresh], 不会显示加载中布局, 也不执行[onLoading]
         * @param refresh 是否回调[onRefresh]
         */
        fun showLoading(
            tag: Any? = null,
            silent: Boolean = false,
            refresh: Boolean = true,
        ) {
            if (silent && refresh) {
                onRefresh?.invoke(this, tag)
                return
            }
            if (status == LOADING) {
                onLoading?.invoke(getStatusView(LOADING, tag), tag)
                return
            }
            showStatus(LOADING, tag)
            if (refresh) onRefresh?.invoke(this, tag)
        }

        /**
         * 静默刷新, 仅触发[onRefresh], 不会改变缺省页状态
         */
        fun refresh() {
            showLoading(silent = true)
        }

        /**
         * 初次加载使用[showLoading], 加载成功过一次以后只会静默加载[refresh]
         *
         * @param tag 传递任意对象给[onLoading]函数
         */
        fun refreshing(tag: Any? = null) {
            if (loaded) {
                refresh()
            } else {
                showLoading(tag)
            }
        }

        /**
         * 显示空页, 会触发[onEmpty]的函数参数
         * @param tag 传递任意对象给[onEmpty]函数
         */
        fun showEmpty(tag: Any? = null) {
            showStatus(EMPTY, tag)
        }

        /**
         * 显示错误页, 会触发[onError]的函数参数
         * @param tag 传递任意对象给[onError]函数
         */
        fun showError(tag: Any? = null) {
            showStatus(ERROR, tag)
        }

        /**
         * 显示内容布局, 表示成功缺省页
         * @param tag 传递任意对象给[onContent]函数
         */
        fun showContent(tag: Any? = null) {
            if (trigger && stateChanged) return
            showStatus(CONTENT, tag)
            loaded = true
        }

        // </editor-fold>

        /**
         * 会为所有[StateLayout.emptyLayout]/[StateLayout.errorLayout]中的指定Id的视图对象添加一个点击事件
         * 该点击事件会触发[StateLayout.showLoading], 同时500ms内防抖动
         *
         * @see isNetworkingRetry 点击重试是否检查网络
         */
        fun setRetryIds(
            @IdRes vararg ids: Int,
        ) = apply {
            retryIds = ids
        }

        /**
         * 本函数为方便其他框架热插拔使用, 开发者一般情况不使用
         * 本函数调用两次之间显示缺省页只会有效执行一次
         */
        fun trigger(): Boolean {
            trigger = !trigger
            if (!trigger) stateChanged = false
            return trigger
        }

        /**
         * 显示视图
         */
        private fun showStatus(
            status: Status,
            tag: Any? = null,
        ) {
            if (trigger) stateChanged = true
            if (this.status == status) return
            val previousStatus = this.status
            this.status = status
            runMain {
                try {
                    val targetStatusView = getStatusView(status, tag)
                    statusContainer.filter {
                        it.key != status
                    }.forEach {
                        val statusBundle = it.value
                        if (it.key == previousStatus) {
                            stateChangedHandler?.onRemove(this, statusBundle.view, it.key, statusBundle.tag)
                        }
                    }
                    stateChangedHandler?.onAdd(this, targetStatusView, status, tag)
                    if (status == EMPTY || status == ERROR) {
                        if (canRetry) {
                            retryIds?.forEach {
                                targetStatusView.findViewById<View>(it)?.throttleClick {
                                    showLoading(tag = statusContainer[LOADING]?.tag, silent = isNetworkingRetry && !isNetworking())
                                }
                            }
                        }
                    }
                    when (status) {
                        EMPTY -> onEmpty?.invoke(targetStatusView, tag)
                        ERROR -> onError?.invoke(targetStatusView, tag)
                        LOADING -> onLoading?.invoke(targetStatusView, tag)
                        CONTENT -> onContent?.invoke(targetStatusView, tag)
                    }
                } catch (e: Exception) {
                    Log.e(javaClass.simpleName, "", e)
                }
            }
        }

        /**
         * 删除指定的缺省页
         */
        private fun removeStatus(status: Status) {
            statusContainer.remove(status)
        }

        /**
         * 返回缺省页视图对象
         */
        @Throws(NullPointerException::class)
        private fun getStatusView(
            status: Status,
            tag: Any?,
        ): View {
            statusContainer[status]?.let {
                it.tag = tag
                return it.view
            }
            val layoutId =
                when (status) {
                    EMPTY -> emptyLayout
                    ERROR -> errorLayout
                    LOADING -> loadingLayout
                    CONTENT -> NO_ID
                }
            if (layoutId == NO_ID) {
                when (status) {
                    ERROR -> throw Resources.NotFoundException("No StateLayout errorLayout is set")
                    EMPTY -> throw Resources.NotFoundException("No StateLayout emptyLayout is set")
                    LOADING -> throw Resources.NotFoundException("No StateLayout loadingLayout is set")
                    CONTENT -> throw Resources.NotFoundException("No StateLayout contentView is set")
                }
            }
            val view = LayoutInflater.from(context).inflate(layoutId, this, false)
            statusContainer[status] = StatusInfo(view, tag)
            return view
        }

        /**
         * 标记视图为内容布局, 本函数为其他框架进行热插拔适配使用, 一般情况开发者不使用
         */
        fun setContent(view: View) {
            statusContainer[CONTENT] = StatusInfo(view, null)
        }

        /**
         * 是否处于联网中
         */
        private fun isNetworking(): Boolean {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            return try {
                val network = connectivityManager.activeNetwork ?: return false
                val actNw = connectivityManager.getNetworkCapabilities(network) ?: return false
                when {
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
                    actNw.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
                    else -> false
                }
            } catch (e: Exception) {
                true
            }
        }
    }
