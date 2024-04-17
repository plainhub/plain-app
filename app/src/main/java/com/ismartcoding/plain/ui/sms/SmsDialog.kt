package com.ismartcoding.plain.ui.sms

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.brv.utils.bindingAdapter
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.ActionSourceType
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.PermissionsResultEvent
import com.ismartcoding.plain.features.sms.DMessage
import com.ismartcoding.plain.features.sms.SmsMediaStoreHelper
import com.ismartcoding.plain.ui.BaseListDrawerDialog
import com.ismartcoding.plain.ui.extensions.checkPermission
import com.ismartcoding.plain.ui.extensions.checkable
import com.ismartcoding.plain.ui.helpers.BottomMenuHelper
import com.ismartcoding.plain.ui.models.DataModel
import com.ismartcoding.plain.ui.models.DrawerMenuGroupType
import kotlinx.coroutines.launch

class SmsDialog : BaseListDrawerDialog() {
    override val titleId: Int
        get() = R.string.messages_title

    override val dataType: DataType
        get() = DataType.SMS

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        checkPermission()
        initBottomBar(R.menu.action_sms) {
            BottomMenuHelper.onMenuItemClick(viewModel, binding, this)
        }
    }

    override fun initEvents() {
        receiveEvent<PermissionsResultEvent> {
            checkPermission()
        }
        receiveEvent<ActionEvent> { event ->
            if (event.source == ActionSourceType.SMS) {
                binding.list.page.refresh()
            }
        }
    }

    override fun initTopAppBar() {
        initTopAppBar(R.menu.sms_items) {
        }
    }

    override fun initList() {
        val rv = binding.list.rv
        rv.linear().setup {
            addType<DataModel>(R.layout.item_sms)

            R.id.container.onLongClick {
                viewModel.toggleMode.value = true
                rv.bindingAdapter.setChecked(bindingAdapterPosition, true)
            }

            checkable(onItemClick = {
                val d = getModel<DataModel>().data as DMessage
//                PlainTextDialog(d.address, d.body).show()
            }, onChecked = {
                updateBottomActions()
                updateTitle()
            })
        }

        initRefreshLoadMore()
    }

    private fun checkPermission() {
        binding.list.checkPermission(requireContext(), AppFeatureType.SMS)
    }

    override fun updateList() {
        lifecycleScope.launch {
            val query = viewModel.getQuery()
            val items = withIO { SmsMediaStoreHelper.search(requireContext(), query, viewModel.limit, viewModel.offset) }
            viewModel.total = withIO { SmsMediaStoreHelper.count(requireContext(), query) }

            val bindingAdapter = binding.list.rv.bindingAdapter
            val toggleMode = bindingAdapter.toggleMode
            val checkedItems = bindingAdapter.getCheckedModels<DataModel>()
            binding.list.page.addData(
                items.map { a ->
                    DataModel(a).apply {
                        keyText = a.address
                        valueText = a.date.formatDateTime()
                        subtitle = a.body
                        this.toggleMode = toggleMode
                        isChecked = checkedItems.any { it.data.id == data.id }
                    }
                },
                hasMore = {
                    items.size == viewModel.limit
                },
            )
            updateTitle()
        }
    }

    override fun updateDrawerMenu() {
        updateDrawerMenu(DrawerMenuGroupType.ALL, DrawerMenuGroupType.SMS_TYPES, DrawerMenuGroupType.TAGS)
    }
}
