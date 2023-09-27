package com.ismartcoding.plain.ui.route

import android.os.Bundle
import android.view.View
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.databinding.DialogRouteTargetBinding
import com.ismartcoding.plain.features.TargetType
import com.ismartcoding.plain.ui.BaseBottomSheetDialog
import com.ismartcoding.plain.ui.SelectNetworkDialog
import com.ismartcoding.plain.ui.extensions.initView
import com.ismartcoding.plain.ui.extensions.setSafeClick
import com.ismartcoding.plain.ui.views.ChipItem

class RouteTargetDialog(
    var type: TargetType,
    var value: String = "",
    val onDone: (TargetType, String) -> Unit,
) : BaseBottomSheetDialog<DialogRouteTargetBinding>() {
    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        updateUI()
    }

    private fun hasInputValue(type: TargetType): Boolean {
        return arrayOf(TargetType.IP, TargetType.NET, TargetType.DNS, TargetType.REMOTE_PORT).contains(type)
    }

    private fun hasSelectValue(type: TargetType): Boolean {
        return arrayOf(TargetType.INTERFACE, TargetType.LIST).contains(type)
    }

    private fun updateValueInput() {
        when {
            hasInputValue(type) -> {
                binding.value.run {
                    visibility = View.VISIBLE
                    hint = type.getText()
                    text = value
                    placeholderText = type.getPlaceholder()
                    helperText = type.getExamples()
                    onValidate = {
                        type.validate(it)
                    }
                    onTextChanged = {
                        value = it
                    }
                }
                binding.selectValue.visibility = View.GONE
            }
            hasSelectValue(type) -> {
                binding.value.visibility = View.GONE
                // if doesn't exist, set it to empty
                if (value.isNotEmpty() && !UIDataCache.current().getInterfaces("").any { it.name == value }) {
                    value = ""
                }

                binding.selectValue.run {
                    visibility = View.VISIBLE
                    selectValue = value
                    setKeyText(type.getText())
                    updateSelectValueRow()
                    setClick {
                        SelectNetworkDialog {
                            selectValue = it
                            value = it
                            updateSelectValueRow()
                        }.show()
                    }
                }
            }
            else -> {
                binding.value.visibility = View.GONE
                binding.selectValue.visibility = View.GONE
            }
        }
    }

    private fun updateSelectValueRow() {
        binding.selectValue.run {
            if (type == TargetType.INTERFACE) {
                isRequired = false
                showMore()
                setValueText(
                    com.ismartcoding.plain.features.Target(TargetType.INTERFACE, value).getText(UIDataCache.current().getNetworks()),
                )
                setValueTextColor(R.color.primary)
            } else {
                isRequired = true
                if (value.isEmpty()) {
                    hideEndIcon()
                    setSelect()
                } else {
                    showMore()
                    setValueText(value)
                    setValueTextColor(R.color.primary)
                }
            }
        }
    }

    private fun updateUI() {
        updateValueInput()
        binding.types.initView(ChipItem.getRouteTargetTypes(), type.value) { v ->
            type = TargetType.parse(v)
            updateValueInput()
        }

        binding.button.setSafeClick {
            if (hasInputError()) {
                return@setSafeClick
            }

            onDone(type, if (hasInputValue(type) || hasSelectValue(type)) value else "")
            dismiss()
        }

        addFormItem(binding.value)
        addFormItem(binding.selectValue)
    }
}
