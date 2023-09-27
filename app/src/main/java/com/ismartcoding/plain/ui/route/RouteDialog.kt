package com.ismartcoding.plain.ui.route

import android.os.Bundle
import android.view.View
import androidx.lifecycle.lifecycleScope
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.CreateConfigMutation
import com.ismartcoding.plain.R
import com.ismartcoding.plain.UpdateConfigMutation
import com.ismartcoding.plain.api.BoxApi
import com.ismartcoding.plain.data.*
import com.ismartcoding.plain.data.enums.ActionSourceType
import com.ismartcoding.plain.data.enums.ActionType
import com.ismartcoding.plain.data.preference.DeviceSortByPreference
import com.ismartcoding.plain.databinding.DialogRouteBinding
import com.ismartcoding.plain.extensions.sorted
import com.ismartcoding.plain.extensions.toRoute
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.route.Route
import com.ismartcoding.plain.features.route.RouteEdit
import com.ismartcoding.plain.features.route.toRouteEdit
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.SelectInterfaceDialog
import com.ismartcoding.plain.ui.SelectItemDialog
import com.ismartcoding.plain.ui.extensions.setSafeClick
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.views.LoadingButtonView
import kotlinx.coroutines.launch

class RouteDialog(private var mItem: Route?) : BaseBottomSheetDialog<DialogRouteBinding>() {
    private lateinit var routeEdit: RouteEdit

    override fun getSubmitButton(): LoadingButtonView {
        return binding.button
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        updateUI()
        addFormItem(binding.target)
        addFormItem(binding.ifName)
        addFormItem(binding.notes)
    }

    private fun updateUI() {
        routeEdit = mItem?.toRouteEdit() ?: RouteEdit.createDefault()

        binding.topAppBar.title = if (mItem == null) getString(R.string.create_route) else getString(R.string.edit_route)

        updateTargetRow()
        updateViaRow()
        updateApplyToRow()

        binding.notes.run {
            text = routeEdit.notes
            onBlur = { _, v ->
                routeEdit.notes = v
            }
        }

        binding.button.setSafeClick {
            if (hasInputError()) {
                return@setSafeClick
            }

            lifecycleScope.launch {
                if (mItem == null) {
                    doCreateRouteAsync()
                } else {
                    doUpdateRouteAsync()
                }
            }
        }
    }

    private suspend fun doCreateRouteAsync() {
        blockFormUI()
        val r =
            withIO {
                BoxApi.mixMutateAsync(CreateConfigMutation(routeEdit.toRouteInput()))
            }
        unblockFormUI()
        if (!r.isSuccess()) {
            DialogHelper.showErrorDialog(requireContext(), r.getErrorMessage())
            return
        }

        r.response?.data?.createConfig?.let {
            UIDataCache.current().routes?.add(it.configFragment.toRoute())
            sendEvent(ActionEvent(ActionSourceType.ROUTE, ActionType.CREATED, setOf(it.configFragment.id)))
        }

        dismiss()
    }

    private suspend fun doUpdateRouteAsync() {
        blockFormUI()
        val r =
            withIO {
                BoxApi.mixMutateAsync(UpdateConfigMutation(mItem!!.id, routeEdit.toRouteInput()))
            }
        unblockFormUI()
        if (!r.isSuccess()) {
            DialogHelper.showErrorDialog(requireContext(), r.getErrorMessage())
            return
        }

        r.response?.data?.updateConfig?.let {
            UIDataCache.current().routes?.run {
                val index = indexOfFirst { it.id == mItem!!.id }
                if (index != -1) {
                    removeAt(index)
                    add(index, it.configFragment.toRoute())
                } else {
                    add(it.configFragment.toRoute())
                }
            }
            sendEvent(ActionEvent(ActionSourceType.ROUTE, ActionType.UPDATED, setOf(mItem!!.id)))
        }
        dismiss()
    }

    private fun updateTargetRow() {
        binding.target.run {
            if (routeEdit.target.isEmpty()) {
                setSelect()
            } else {
                showMore()
                selectValue = routeEdit.target.toValue()
                setValueText(routeEdit.target.getText(UIDataCache.current().getNetworks()))
                setValueTextColor(R.color.primary)
                validate()
            }

            setClick {
                RouteTargetDialog(routeEdit.target.type, routeEdit.target.value) { type, value ->
                    routeEdit.target.type = type
                    routeEdit.target.value = value
                    updateTargetRow()
                }.show()
            }
        }
    }

    private fun updateViaRow() {
        binding.ifName.run {
            if (routeEdit.ifName.isEmpty()) {
                setSelect()
            } else {
                showMore()
                selectValue = routeEdit.ifName
                setValueText(routeEdit.ifDisplayName())
                setValueTextColor(R.color.primary)
                validate()
            }

            setClick {
                SelectInterfaceDialog(UIDataCache.current().getNetworks().filter { setOf("wan", "vpn").contains(it.type) }) {
                    routeEdit.ifName = it
                    updateViaRow()
                }.show()
            }
        }
    }

    private fun updateApplyToRow() {
        binding.applyTo.run {
            setValueText(routeEdit.applyTo.getText(UIDataCache.current().getDevices(), UIDataCache.current().getNetworks()))
            setClick {
                SelectItemDialog(search = { q ->
                    val items = mutableListOf<Any>()
                    items.add(AllItemsOption(getString(R.string.all_devices)))
                    items.addAll(UIDataCache.current().getNetworks(q).filter { !setOf("wan", "vpn").contains(it.type) })
                    items.addAll(UIDataCache.current().getDevices(q).sorted(DeviceSortByPreference.getValueAsync(requireContext())))
                    items
                }) { type, value ->
                    routeEdit.applyTo.type = type
                    routeEdit.applyTo.value = value
                    updateApplyToRow()
                }.show()
            }
        }
    }
}
