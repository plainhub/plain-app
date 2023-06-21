package com.ismartcoding.plain.ui.home

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.Menu
import android.view.View
import androidx.appcompat.widget.PopupMenu
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import com.ismartcoding.lib.brv.BindingAdapter
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.models
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.extensions.allowSensitivePermissions
import com.ismartcoding.lib.extensions.setSelectableItemBackground
import com.ismartcoding.lib.roundview.RoundLinearLayout
import com.ismartcoding.lib.roundview.setStrokeColor
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.*
import com.ismartcoding.plain.features.HomeItemRefreshEvent
import com.ismartcoding.plain.features.HomeItemType
import com.ismartcoding.plain.ui.home.views.bindData
import com.ismartcoding.plain.ui.home.views.initEvents
import com.ismartcoding.plain.ui.home.views.initView
import kotlinx.coroutines.Job

data class HomeItemModel(val type: HomeItemType, val events: MutableList<Job> = mutableListOf())

class HomeView(context: Context, attrs: AttributeSet? = null) : RecyclerView(context, attrs), LifecycleObserver {
    private val pool = RecycledViewPool()
    private val events = mutableListOf<Job>()

    private fun registerLifecycleOwner(lifecycle: Lifecycle) {
        lifecycle.addObserver(this)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        events.forEach {
            it.cancel()
        }
    }


    fun isScrollable(): Boolean {
        return canScrollHorizontally(1)
    }

    fun initView(lifecycle: Lifecycle) {
        registerLifecycleOwner(lifecycle)

        linear().adapter = object : BindingAdapter() {
            override fun onViewRecycled(holder: BindingViewHolder) {
                val m = holder._data as HomeItemModel
                m.events.forEach {
                    it.cancel()
                }
                m.events.clear()
            }
        }.apply {
            addType<HomeItemModel> {
                when (type) {
                    HomeItemType.STORAGE -> {
                        R.layout.home_item_storage
                    }
                    HomeItemType.EXCHANGE -> {
                        R.layout.home_item_exchange
                    }
                    HomeItemType.NETWORK -> {
                        R.layout.home_item_network
                    }
                    HomeItemType.EDUCATION -> {
                        R.layout.home_item_education
                    }
                    HomeItemType.WORK -> {
                        R.layout.home_item_work
                    }
                    HomeItemType.SOCIAL -> {
                        R.layout.home_item_social
                    }
                }
            }
            onCreate {
                when (it) {
                    R.layout.home_item_exchange -> {
                        val b = getBinding<HomeItemExchangeBinding>()
                        b.rv.setRecycledViewPool(pool)
                        b.initView()
                    }
                }
            }
            onBind {
                val m = getModel<HomeItemModel>()
                val b: ViewBinding
                when (m.type) {
                    HomeItemType.STORAGE -> {
                        b = getBinding<HomeItemStorageBinding>()
                        b.initView()
                    }
                    HomeItemType.NETWORK -> {
                        b = getBinding<HomeItemNetworkBinding>()
                        b.initEvents(m)
                        b.initView()
                    }
                    HomeItemType.EXCHANGE -> {
                        b = getBinding<HomeItemExchangeBinding>()
                        b.initEvents(context, m)
                        b.bindData(context)
                    }
                    HomeItemType.EDUCATION -> {
                        b = getBinding<HomeItemEducationBinding>()
                        b.initView()
                    }
                    HomeItemType.WORK -> {
                        b = getBinding<HomeItemWorkBinding>()
                        b.initView()
                    }
                    HomeItemType.SOCIAL -> {
                        b = getBinding<HomeItemSocialBinding>()
                        b.initView()
                    }
                }

                itemView.findViewById<RoundLinearLayout>(R.id.section)?.setStrokeColor(context.getColor(R.color.primary))

                itemView.findViewById<View>(R.id.container).run {
                    setSelectableItemBackground()
                    setOnLongClickListener {
                        val popup = PopupMenu(context, itemView)
                        val popupMenu = popup.menu
                        if (m.type.canRefresh()) {
                            addMenuItem(popupMenu, PopupMenuItemType.REFRESH, R.string.refresh)
                        }
                        popup.setOnMenuItemClickListener {
                            when (it.itemId) {
                                PopupMenuItemType.REFRESH.ordinal -> {
                                    sendEvent(HomeItemRefreshEvent(m))
                                }
                            }
                            true
                        }
                        popup.show()
                        true
                    }
                }
            }
        }
    }

    fun update(type: HomeItemType) {
        val index = models?.indexOfFirst { (it as HomeItemModel).type == type } ?: -1
        if (index >= 0) {
            adapter?.notifyItemChanged(index)
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun refreshUI() {
        models = getItems()
    }

    private enum class PopupMenuItemType {
        REFRESH,
    }

    fun getItems(): List<HomeItemModel> {
        val ignore = if (LocalStorage.selectedBoxId.isEmpty()) mutableSetOf(HomeItemType.NETWORK, HomeItemType.EDUCATION) else mutableSetOf()
        if (!context.allowSensitivePermissions()) {
            ignore.add(HomeItemType.SOCIAL)
        }
        val items  = mutableListOf<HomeItemModel>()
        items.addAll(
            HomeItemType.values()
                .filter { !ignore.contains(it) }
                .map {
                    HomeItemModel(it)
                })

        return items
    }

    private fun addMenuItem(menu: Menu, type: PopupMenuItemType, titleRes: Int) {
        menu.add(0, type.ordinal, type.ordinal, titleRes)
    }
}