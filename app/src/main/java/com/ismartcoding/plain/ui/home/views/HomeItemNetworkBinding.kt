package com.ismartcoding.plain.ui.home.views

import com.ismartcoding.lib.channel.receiveEventHandler
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.LocalStorage
import com.ismartcoding.plain.R
import com.ismartcoding.plain.data.*
import com.ismartcoding.plain.data.enums.ActionSourceType
import com.ismartcoding.plain.databinding.HomeItemNetworkBinding
import com.ismartcoding.plain.features.ActionEvent
import com.ismartcoding.plain.features.ChatItemRefreshEvent
import com.ismartcoding.plain.features.HomeItemType
import com.ismartcoding.plain.features.box.FetchNetworksEvent
import com.ismartcoding.plain.features.box.NetworksResultEvent
import com.ismartcoding.plain.features.locale.LocaleHelper
import com.ismartcoding.plain.ui.device.DevicesDialog
import com.ismartcoding.plain.ui.extensions.*
import com.ismartcoding.plain.ui.home.HomeItemModel
import com.ismartcoding.plain.ui.hostapd.HostapdDialog
import com.ismartcoding.plain.ui.network.NetworkConfigDialog
import com.ismartcoding.plain.ui.route.RoutesDialog
import com.ismartcoding.plain.ui.rule.RulesDialog
import com.ismartcoding.plain.ui.wireguard.WireGuardsDialog

fun HomeItemNetworkBinding.initView() {
    val network = UIDataCache.current().devices
    if (network == null) {
        sendEvent(FetchNetworksEvent(LocalStorage.selectedBoxId))
        state.showLoading()
    }
    updateUI()
}

private fun HomeItemNetworkBinding.updateUI() {
    title.setTextColor(title.context.getColor(R.color.primary))
    title.setText(R.string.home_item_title_network)

    networkConfig
        .initTheme()
        .setKeyText(R.string.network_config)
        .showMore()
        .setClick {
            NetworkConfigDialog().show()
        }

    wifi
        .initTheme()
        .setKeyText(R.string.wifi)
        .showMore()
        .setClick {
            HostapdDialog().show()
        }
    wireguard
        .initTheme()
        .setKeyText(R.string.wireguard)
        .setValueText(UIDataCache.current().wireGuards?.size?.toString() ?: "")
        .showMore()
        .setClick {
            WireGuardsDialog().show()
        }

    rules
        .initTheme()
        .setKeyText(R.string.rules)
        .setValueText(UIDataCache.current().getRules().size.toString())
        .showMore()
        .setClick {
            RulesDialog().show()
        }

    routes
        .initTheme()
        .setKeyText(R.string.routes)
        .setValueText(UIDataCache.current().getRoutes().size.toString())
        .showMore()
        .setClick {
            RoutesDialog().show()
        }

    val deviceList = UIDataCache.current().getDevices()
    devices
        .initTheme()
        .setKeyText(R.string.devices)
        .setValueText(LocaleHelper.getStringF(R.string.online_total, "total", deviceList.size, "online", deviceList.count { it.isOnline }))
        .showMore()
        .setClick {
            DevicesDialog().show()
        }
}

fun HomeItemNetworkBinding.initEvents(m: HomeItemModel) {
    m.events.add(receiveEventHandler<ChatItemRefreshEvent> { event ->
        if (event.data.type == HomeItemType.NETWORK.value) {
            state.showLoading()
            sendEvent(FetchNetworksEvent(LocalStorage.selectedBoxId))
        }
    })

    m.events.add(receiveEventHandler<ActionEvent> { event ->
        if (setOf(ActionSourceType.ROUTE, ActionSourceType.RULE, ActionSourceType.DEVICE).contains(event.source)) {
            updateUI()
        }
    })

    m.events.add(receiveEventHandler<NetworksResultEvent> { event ->
        state.update(event.result) {
            sendEvent(FetchNetworksEvent(LocalStorage.selectedBoxId))
        }
        updateUI()
    })
}