package com.ismartcoding.plain.ui

import android.os.Bundle
import android.view.MenuItem
import android.view.View
import androidx.annotation.MenuRes
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.brv.utils.bindingAdapter
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.coMain
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.data.enums.ActionSourceType
import com.ismartcoding.plain.data.enums.ActionType
import com.ismartcoding.plain.data.enums.DataType
import com.ismartcoding.plain.databinding.DialogListDrawerBinding
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.extensions.*
import com.ismartcoding.plain.ui.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

abstract class BaseListDrawerDialog : BaseDialog<DialogListDrawerBinding>() {
    protected val viewModel: FilteredItemsViewModel by viewModels()

    protected abstract val titleId: Int
    protected abstract val dataType: DataType

    protected abstract fun initEvents()

    protected abstract fun initTopAppBar()

    protected abstract fun initList()

    protected abstract fun updateList()

    protected abstract fun updateDrawerMenu()

    override fun getTheme(): Int {
        return R.style.Theme_Plain_TransparentBar
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        viewModel.dataType = dataType

        binding.list.page.pageName = this.javaClass.simpleName
        binding.initToggleMode(viewLifecycleOwner, viewModel, titleId)
        initBasicEvents()
        initList()
        initEvents()
        initTopAppBar()

        initDrawerMenu()
        updateDrawerMenu()
    }

    override fun onBackPressed() {
        if (binding.drawer.isOpen) {
            binding.drawer.close()
        } else if (viewModel.toggleMode.value == true) {
            viewModel.toggleMode.value = false
        } else {
            dismiss()
        }
    }

    protected open fun initDrawerMenu() {
        binding.drawerContent.rv.initDrawerMenu()
    }

    open fun initBasicEvents() {
        receiveEvent<DrawerMenuItemClickedEvent> { event ->
            val m = event.model
            viewModel.offset = 0
            viewModel.trash.value = m.iconId == R.drawable.ic_trash
            viewModel.data = m.data as? IData
            binding.drawer.close()
            binding.topAppBar.layout.setExpanded(true)
            initTopAppBar()
            updateTitle()
            coMain {
                delay(250) // wait until the drawer is closed to make sure the animation is smooth on some phones.
                binding.list.page.showLoading()
            }
        }
        receiveEvent<ActionEvent> { event ->
            if (event.source == ActionSourceType.TAG_RELATION) {
                binding.list.rv.bindingAdapter.checkedAll(false)
                binding.list.page.refresh()
                updateDrawerMenu()
            } else if (setOf(ActionSourceType.TAG, ActionSourceType.FEED).contains(event.source)) {
                if (event.action == ActionType.DELETED && viewModel.data != null && event.ids.contains(viewModel.data!!.id)) {
                    viewModel.data = null
                }
                updateDrawerMenu()
                if (event.action != ActionType.CREATED) {
                    binding.list.page.refresh()
                }
            }
        }
    }

    protected fun initBottomBar(
        @MenuRes menuId: Int,
        menuItemClick: MenuItem.() -> Unit,
    ) {
        binding.bottomAction.run {
            initMenu(menuId)
            onMenuItemClick {
                menuItemClick(this)
            }
        }
    }

    protected fun initTopAppBar(
        @MenuRes menuId: Int,
        menuItemClick: MenuItem.() -> Unit,
    ) {
        binding.topAppBar.toolbar.run {
            initMenu(menuId)

            val isVisible = viewModel.data == null || viewModel.data !is DMediaFolders
            menu.findItem(R.id.search)?.isVisible = isVisible
            menu.findItem(R.id.more)?.isVisible = isVisible

            onBack {
                onBackPressed()
            }

            onSearch { q ->
                if (viewModel.searchQ != q) {
                    viewModel.searchQ = q
                    viewModel.offset = 0
                    binding.list.page.refresh()
                }
            }

            onMenuItemClick {
                val list = binding.list
                val rv = list.rv
                when (itemId) {
                    R.id.menu -> {
                        binding.drawer.open()
                    }
                    R.id.select_all -> {
                        if (title == LocaleHelper.getString(R.string.select_all)) {
                            setTitle(R.string.unselect_all)
                            rv.bindingAdapter.checkedAll(true)
                        } else {
                            setTitle(R.string.select_all)
                            rv.bindingAdapter.checkedAll(false)
                        }
                    }
                    R.id.cast_mode -> {
                        viewModel.castMode = !this.isChecked
                        this.isChecked = !this.isChecked
                    }
                    else -> {
                        menuItemClick(this)
                    }
                }
            }
        }
    }

    protected fun updateDrawerMenu(vararg types: DrawerMenuGroupType) {
        lifecycleScope.launch {
            binding.drawerContent.rv.updateDrawerMenuAsync(viewModel, *types)
        }
    }

    protected open fun updateBottomActions() {
        val count = binding.list.rv.bindingAdapter.getCheckedModels<IDataModel>().size
        if (count > 0) {
            binding.bottomAction.performShow()
            binding.bottomAction.menu.run {
                findItem(R.id.cast)?.isVisible = if (viewModel.dataType == DataType.IMAGE) count == 1 else true
                findItem(R.id.call)?.isVisible = count == 1 // for contactsDialog
            }
        } else {
            binding.bottomAction.performHide()
        }
    }

    protected open fun updateTitle() {
        binding.topAppBar.toolbar.updateTitle(viewModel, binding.list.rv, titleId, viewModel.total)
    }

    protected fun initRefreshLoadMore() {
        binding.list.page.run {
            onRefresh {
                viewModel.offset = 0
                updateList()
            }

            setEnableLoadMore(true)
            onLoadMore {
                viewModel.offset += viewModel.limit
                updateList()
            }
        }
    }
}
