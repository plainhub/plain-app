package com.ismartcoding.plain.features.network

import android.content.Context
import com.ismartcoding.plain.data.UIDataCache
import com.ismartcoding.plain.databinding.ViewListItemBinding
import com.ismartcoding.plain.fragment.NetworkFragment
import com.ismartcoding.plain.ui.extensions.addTextRow
import com.ismartcoding.plain.ui.extensions.clearTextRows
import com.ismartcoding.plain.ui.extensions.setKeyText

fun ViewListItemBinding.bindNetwork(
    context: Context,
    item: NetworkFragment,
) {
    clearTextRows()
    setKeyText(item.name)
    var ip = UIDataCache.current().getInterfaces().find { it.name == item.ifName }?.ip4 ?: ""
    if (ip.isEmpty() && item.type == "vpn") {
        ip =
            UIDataCache.current().wireGuards?.find { it.id == item.ifName }?.interfaze?.addresses?.joinToString(
                ", ",
            ) ?: ""
    }
    addTextRow(ip)
}
