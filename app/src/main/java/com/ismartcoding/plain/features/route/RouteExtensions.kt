package com.ismartcoding.plain.features.route

import android.content.Context
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
import com.ismartcoding.plain.ui.route.RouteDialog
import kotlinx.coroutines.launch

fun Route.toRouteEdit(): RouteEdit {
    return RouteEdit(
        ApplyTo.parse(applyTo),
        Target.parse(target),
        gateway,
        ifName,
        isEnabled,
        notes,
    )
}

fun ViewListItemBinding.bindRoute(
    context: Context,
    lifecycleScope: LifecycleCoroutineScope,
    item: Route,
) {
    clearTextRows()
    setKeyText(item.getTitle())
    addTextRow(item.getMessage())
    setClick {
        RouteDialog(item).show()
    }
    setSwitch(item.isEnabled, onChanged = { _, isEnabled ->
        lifecycleScope.launch {
            DialogHelper.showLoading()
            item.isEnabled = isEnabled
            val r =
                withIO {
                    BoxApi.mixMutateAsync(UpdateConfigMutation(item.id, item.toRouteEdit().toRouteInput()))
                }
            DialogHelper.hideLoading()
            if (!r.isSuccess()) {
                DialogHelper.showErrorDialog(context, r.getErrorMessage())
                item.isEnabled = !isEnabled
                setSwitchEnable(!isEnabled)
                return@launch
            }

            r.response?.data?.updateConfig?.let {
                UIDataCache.current().routes?.run {
                    val index = indexOfFirst { it.id == item.id }
                    if (index != -1) {
                        removeAt(index)
                        add(index, it.configFragment.toRoute())
                    } else {
                        add(it.configFragment.toRoute())
                    }
                }
            }
        }
    })
}

fun Route.getTitle(): String {
    return ApplyTo.parse(applyTo).getText(UIDataCache.current().getDevices(), UIDataCache.current().getNetworks())
}

fun Route.getMessage(): String {
    val targetData = Target.parse(target)
    return LocaleHelper.getStringF(
        R.string.route_item_description,
        "target",
        targetData.getText(UIDataCache.current().getNetworks()),
        "if_name",
        ifDisplayName(),
    )
}
