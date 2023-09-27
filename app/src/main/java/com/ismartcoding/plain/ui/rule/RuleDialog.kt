package com.ismartcoding.plain.ui.rule

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
import com.ismartcoding.plain.databinding.DialogRuleBinding
import com.ismartcoding.plain.extensions.sorted
import com.ismartcoding.plain.extensions.toRule
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.rule.*
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.SelectItemDialog
import com.ismartcoding.plain.ui.extensions.setSafeClick
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.views.ChipItem
import com.ismartcoding.plain.ui.views.LoadingButtonView
import kotlinx.coroutines.launch

class RuleDialog(private var mItem: Rule?) : BaseBottomSheetDialog<DialogRuleBinding>() {
    private lateinit var ruleEdit: RuleEdit

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
        addFormItem(binding.notes)
    }

    private fun updateUI() {
        ruleEdit = mItem?.toRuleEdit() ?: RuleEdit.createDefault()

        binding.topAppBar.title = if (mItem == null) getString(R.string.create_rule) else getString(R.string.edit_rule)

        val ruleActions = ChipItem.getRuleActions()
        binding.action.setChips(ruleActions, ruleEdit.action.value) {
            ruleEdit.action = RuleAction.parse(it)
        }

        val ruleDirections = ChipItem.getRuleDirections()
        binding.direction.setChips(ruleDirections, ruleEdit.direction.value) {
            ruleEdit.direction = RuleDirection.parse(it)
        }

        updateTargetRow()
        updateApplyToRow()

        binding.notes.run {
            text = ruleEdit.notes
            onBlur = { _, v ->
                ruleEdit.notes = v
            }
        }

        binding.button.setSafeClick {
            if (hasInputError()) {
                return@setSafeClick
            }

            lifecycleScope.launch {
                if (mItem == null) {
                    doCreateRuleAsync()
                } else {
                    doUpdateRuleAsync()
                }
            }
        }
    }

    private suspend fun doCreateRuleAsync() {
        blockFormUI()
        val r =
            withIO {
                BoxApi.mixMutateAsync(CreateConfigMutation(ruleEdit.toRuleInput()))
            }
        unblockFormUI()
        if (!r.isSuccess()) {
            DialogHelper.showErrorDialog(requireContext(), r.getErrorMessage())
            return
        }

        r.response?.data?.createConfig?.let {
            UIDataCache.current().rules?.add(it.configFragment.toRule())
            sendEvent(ActionEvent(ActionSourceType.RULE, ActionType.CREATED, setOf(it.configFragment.id)))
        }

        dismiss()
    }

    private suspend fun doUpdateRuleAsync() {
        blockFormUI()
        val r =
            withIO {
                BoxApi.mixMutateAsync(UpdateConfigMutation(mItem!!.id, ruleEdit.toRuleInput()))
            }
        unblockFormUI()
        if (!r.isSuccess()) {
            DialogHelper.showErrorDialog(requireContext(), r.getErrorMessage())
            return
        }

        r.response?.data?.updateConfig?.let {
            UIDataCache.current().rules?.run {
                val index = indexOfFirst { it.id == mItem!!.id }
                if (index != -1) {
                    removeAt(index)
                    add(index, it.configFragment.toRule())
                } else {
                    add(it.configFragment.toRule())
                }
            }
            sendEvent(ActionEvent(ActionSourceType.RULE, ActionType.UPDATED, setOf(mItem!!.id)))
        }
        dismiss()
    }

    private fun updateTargetRow() {
        binding.target.run {
            if (ruleEdit.target.isEmpty()) {
                setSelect()
            } else {
                showMore()
                selectValue = ruleEdit.target.toValue()
                setValueText(ruleEdit.target.getText(UIDataCache.current().getNetworks()))
                setValueTextColor(R.color.primary)
                validate()
            }

            setClick {
                RuleTargetDialog(ruleEdit.target.type, ruleEdit.target.value, RuleProtocol.ALL) { type, value, protocol ->
                    ruleEdit.target.type = type
                    ruleEdit.target.value = value
                    ruleEdit.protocol = protocol
                    updateTargetRow()
                }.show()
            }
        }
    }

    private fun updateApplyToRow() {
        binding.applyTo.run {
            setValueText(ruleEdit.applyTo.getText(UIDataCache.current().getDevices(), UIDataCache.current().getNetworks()))
            setClick {
                SelectItemDialog(search = { q ->
                    val items = mutableListOf<Any>()
                    items.add(AllItemsOption(getString(R.string.all_devices)))
                    items.addAll(UIDataCache.current().getSelectableNetworks(q))
                    items.addAll(UIDataCache.current().getDevices(q).sorted(DeviceSortByPreference.getValueAsync(requireContext())))
                    items
                }) { type, value ->
                    ruleEdit.applyTo.type = type
                    ruleEdit.applyTo.value = value
                    updateApplyToRow()
                }.show()
            }
        }
    }
}
