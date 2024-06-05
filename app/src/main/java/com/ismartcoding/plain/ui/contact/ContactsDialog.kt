package com.ismartcoding.plain.ui.contact

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.brv.utils.bindingAdapter
import com.ismartcoding.lib.brv.utils.linear
import com.ismartcoding.lib.brv.utils.setup
import com.ismartcoding.lib.channel.receiveEvent
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.pinyin.Pinyin
import com.ismartcoding.plain.R
import com.ismartcoding.plain.enums.ActionSourceType
import com.ismartcoding.plain.enums.ActionType
import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.PermissionsResultEvent
import com.ismartcoding.plain.features.media.CallMediaStoreHelper
import com.ismartcoding.plain.features.media.ContactMediaStoreHelper
import com.ismartcoding.plain.data.DContact
import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.ui.BaseListDrawerDialog
import com.ismartcoding.plain.ui.extensions.checkPermission
import com.ismartcoding.plain.ui.extensions.checkable
import com.ismartcoding.plain.ui.extensions.ensureSelect
import com.ismartcoding.plain.ui.helpers.BottomMenuHelper
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.models.DataModel
import com.ismartcoding.plain.ui.models.DrawerMenuGroupType
import kotlinx.coroutines.launch

class ContactsDialog : BaseListDrawerDialog() {
    override val titleId: Int
        get() = R.string.contacts_title

    override val dataType: DataType
        get() = DataType.CONTACT

    private var phoneNumberToCall = ""

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        checkPermission()
        initBottomBar(R.menu.action_contacts) {
            when (itemId) {
                R.id.call -> {
                    binding.list.rv.ensureSelect { items ->
                        val m = items[0]
                        val contact = m.data as DContact
                        if (contact.phoneNumbers.isNotEmpty()) {
                            phoneNumberToCall = contact.phoneNumbers[0].value
                            if (Permission.CALL_PHONE.can(requireContext())) {
                                CallMediaStoreHelper.call(requireContext(), phoneNumberToCall)
                            } else {
                                Permission.CALL_PHONE.grant(requireContext())
                            }
                        }
                    }
                }

                R.id.delete -> {
                    if (!Permission.WRITE_CONTACTS.can(requireContext())) {
                        Permission.WRITE_CONTACTS.grant(requireContext())
                        return@initBottomBar
                    }
                    binding.list.rv.ensureSelect { items ->
                        DialogHelper.confirmToDelete {
                            lifecycleScope.launch {
                                val ids = items.map { it.data.id }.toSet()
                                DialogHelper.showLoading()
                                withIO {
                                    TagHelper.deleteTagRelationByKeys(ids, DataType.CONTACT)
                                    ContactMediaStoreHelper.deleteByIdsAsync(requireContext(), ids)
                                }
                                DialogHelper.hideLoading()
                                binding.list.rv.bindingAdapter.checkedAll(false)
                                sendEvent(ActionEvent(ActionSourceType.CONTACT, ActionType.DELETED, ids))
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

    override fun initTopAppBar() {
        initTopAppBar(R.menu.contacts) {
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
            }
        }
        receiveEvent<PermissionsResultEvent> {
            checkPermission()
        }
        receiveEvent<ActionEvent> { event ->
            if (event.source == ActionSourceType.CONTACT) {
                binding.list.page.refresh()
            }
        }
    }

    private fun checkPermission() {
        binding.list.checkPermission(requireContext(), AppFeatureType.CONTACTS)
    }

    override fun updateList() {
        viewModel.limit = 5000
        lifecycleScope.launch {
            val query = viewModel.getQuery()
            val items =
                withIO {
                    ContactMediaStoreHelper.searchAsync(
                        requireContext(),
                        query,
                        viewModel.limit,
                        viewModel.offset,
                    ).sortedBy { Pinyin.toPinyin(it.fullName()) }
                }
            viewModel.total = withIO { ContactMediaStoreHelper.countAsync(requireContext(), query) }

            val bindingAdapter = binding.list.rv.bindingAdapter
            val toggleMode = bindingAdapter.toggleMode
            val checkedItems = bindingAdapter.getCheckedModels<DataModel>()
            binding.list.page.addData(
                items.map { a ->
                    DataModel(a).apply {
                        keyText = a.fullName()
                        subtitle = a.phoneNumbers.map { it.value }.distinct().joinToString(", ")
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
        updateDrawerMenu(DrawerMenuGroupType.ALL, DrawerMenuGroupType.TAGS)
    }

    override fun initList() {
        val rv = binding.list.rv
        rv.linear().setup {
            addType<DataModel>(R.layout.item_row)

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
