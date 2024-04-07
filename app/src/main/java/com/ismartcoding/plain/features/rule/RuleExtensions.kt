package com.ismartcoding.plain.features.rule

import androidx.lifecycle.LifecycleCoroutineScope
import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.plain.R
import com.ismartcoding.plain.UpdateConfigMutation
import com.ismartcoding.plain.api.BoxApi
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.databinding.ViewListItemBinding
import com.ismartcoding.plain.extensions.*
import com.ismartcoding.plain.features.ApplyTo
import com.ismartcoding.plain.features.Target
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.extensions.*
import com.ismartcoding.plain.ui.helpers.DialogHelper
import com.ismartcoding.plain.ui.rule.RuleDialog
import kotlinx.coroutines.launch

fun Rule.toRuleEdit(): RuleEdit {
    return RuleEdit(
        RuleAction.parse(action),
        ApplyTo.parse(applyTo),
        localPort,
        RuleDirection.parse(direction),
        RuleProtocol.parse(protocol),
        Target.parse(target),
        isEnabled,
        notes,
    )
}

fun ViewListItemBinding.bindRule(
    lifecycleScope: LifecycleCoroutineScope,
    item: Rule,
) {
    clearTextRows()
    setKeyText(item.getTitle())
    addTextRow(item.getMessage())
    setClick {
        RuleDialog(item).show()
    }
    setSwitch(item.isEnabled, onChanged = { _, isEnabled ->
        lifecycleScope.launch {
            DialogHelper.showLoading()
            item.isEnabled = isEnabled
            val r =
                withIO {
                    BoxApi.mixMutateAsync(UpdateConfigMutation(item.id, item.toRuleEdit().toRuleInput()))
                }
            DialogHelper.hideLoading()
            if (!r.isSuccess()) {
                DialogHelper.showErrorDialog(r.getErrorMessage())
                item.isEnabled = !isEnabled
                setSwitchEnable(!isEnabled)
                return@launch
            }

            r.response?.data?.updateConfig?.let {
                UIDataCache.current().rules?.run {
                    val index = indexOfFirst { it.id == item.id }
                    if (index != -1) {
                        removeAt(index)
                        add(index, it.configFragment.toRule())
                    } else {
                        add(it.configFragment.toRule())
                    }
                }
            }
        }
    })
}

fun Rule.getTitle(): String {
    return ApplyTo.parse(applyTo).getText(UIDataCache.current().getDevices(), UIDataCache.current().getNetworks())
}

fun Rule.getMessage(): String {
    val targetData = Target.parse(target)
    val key = if (direction == RuleDirection.INBOUND.value) R.string.rule_item_description_inbound else R.string.rule_item_description_outbound
    return LocaleHelper.getStringF(
        key,
        "action",
        RuleAction.parse(action).getText(),
        "target",
        targetData.getText(UIDataCache.current().getNetworks()),
    )
}
