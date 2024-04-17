package com.ismartcoding.plain.ui.call

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.brv.utils.bindingAdapter
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.ActionSourceType
import com.ismartcoding.plain.enums.ActionType
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.extensions.formatDateTime
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.PermissionsResultEvent
import com.ismartcoding.plain.features.call.CallMediaStoreHelper
import com.ismartcoding.plain.data.DCall
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.ui.BaseListDrawerDialog
import com.ismartcoding.plain.ui.extensions.checkPermission
import com.ismartcoding.plain.ui.extensions.checkable
import com.ismartcoding.plain.ui.extensions.ensureSelect
import com.ismartcoding.plain.ui.helpers.BottomMenuHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.DrawerMenuGroupType
import kotlinx.coroutines.launch

class CallsDialog : BaseListDrawerDialog() {
    override val titleId: Int
        get() = R.string.calls_title

    override val dataType: DataType
        get() = DataType.CALL

    private var phoneNumberToCall = ""

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        checkPermission()
        initBottomBar(R.menu.action_calls) {
            when (itemId) {
                R.id.call -> {
                    binding.list.rv.ensureSelect { items ->
                        val m = items[0]
                        phoneNumberToCall = (m.data as DCall).number
                        if (Permission.CALL_PHONE.can(requireContext())) {
                            CallMediaStoreHelper.call(requireContext(), phoneNumberToCall)
                        } else {
                            Permission.CALL_PHONE.grant(requireContext())
                        }
                    }
                }

                R.id.delete -> {
                    if (!Permission.WRITE_CALL_LOG.can(requireContext())) {
                        Permission.WRITE_CALL_LOG.grant(requireContext())
                        return@initBottomBar
                    }
                    binding.list.rv.ensureSelect { items ->
                        DialogHelper.confirmToDelete {
                            lifecycleScope.launch {
                                val ids = items.map { it.data.id }.toSet()
                                DialogHelper.showLoading()
                                withIO {
                                    TagHelper.deleteTagRelationByKeys(ids, DataType.CALL)
                                    CallMediaStoreHelper.deleteByIds(requireContext(), ids)
                                }
                                DialogHelper.hideLoading()
                                binding.list.rv.bindingAdapter.checkedAll(false)
                                sendEvent(ActionEvent(ActionSourceType.CALL, ActionType.DELETED, ids))
                            }
                        }
                    }
                }

                else -> {
                    BottomMenuHelper.onMenuItemClick(viewModel, binding, this)
                }
            }
        }
    }

    override fun initEvents() {
        receiveEvent<PermissionsResultEvent> { event ->
            if (event.has(Permission.CALL_PHONE)) {
                if (Permission.CALL_PHONE.can(requireContext())) {
                    CallMediaStoreHelper.call(requireContext(), phoneNumberToCall)
                } else {
                    DialogHelper.showMessage(R.string.call_phone_permission_required)
                }
            } else {
                checkPermission()
            }
        }

        receiveEvent<ActionEvent> { event ->
            if (event.source == ActionSourceType.CALL) {
                binding.list.page.refresh()
            }
        }
    }

    override fun initTopAppBar() {
        initTopAppBar(R.menu.calls) {
        }
    }

    private fun checkPermission() {
        binding.list.checkPermission(requireContext(), AppFeatureType.CALLS)
    }

    override fun updateList() {
        lifecycleScope.launch {
            val query = viewModel.getQuery()
            val items = withIO { CallMediaStoreHelper.search(requireContext(), query, viewModel.limit, viewModel.offset) }
            viewModel.total = withIO { CallMediaStoreHelper.count(requireContext(), query) }

            val bindingAdapter = binding.list.rv.bindingAdapter
            val toggleMode = bindingAdapter.toggleMode
            val checkedItems = bindingAdapter.getCheckedModels<CallModel>()
            binding.list.page.addData(
                items.map { a ->
                    CallModel(a).apply {
                        keyText = a.name.ifEmpty { a.number }
                        this.toggleMode = toggleMode
                        subtitle = "${a.startedAt.formatDateTime()} ${getDurationText()} ${getGeoText()}"
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
        updateDrawerMenu(DrawerMenuGroupType.ALL, DrawerMenuGroupType.CALL_TYPES, DrawerMenuGroupType.TAGS)
    }

    override fun initList() {
        val rv = binding.list.rv
        rv.linear().setup {
            addType<CallModel>(R.layout.item_row)

            R.id.container.onLongClick {
                viewModel.toggleMode.value = true
                rv.bindingAdapter.setChecked(bindingAdapterPosition, true)
            }

            checkable(onItemClick = {
            }, onChecked = {
                updateBottomActions()
                updateTitle()
            })
        }

        initRefreshLoadMore()
    }
}
