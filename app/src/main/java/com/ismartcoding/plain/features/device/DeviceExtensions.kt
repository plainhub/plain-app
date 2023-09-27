package com.ismartcoding.plain.features.device

import android.content.Context
import com.ismartcoding.plain.R
import com.ismartcoding.plain.databinding.ViewListItemBinding
import com.ismartcoding.plain.features.locale.LocaleHelper.getString
import com.ismartcoding.plain.fragment.DeviceFragment
import com.ismartcoding.plain.ui.device.DeviceDialog
import com.ismartcoding.plain.ui.extensions.addTextRow
import com.ismartcoding.plain.ui.extensions.clearTextRows
import com.ismartcoding.plain.ui.extensions.setClick
import com.ismartcoding.plain.ui.extensions.setKeyText

fun DeviceFragment.getName(): String {
    if (name.isNullOrEmpty()) {
        return getString(R.string.unknown)
    }

    return name
}

fun ViewListItemBinding.bindDevice(
    context: Context,
    item: DeviceFragment,
) {
    clearTextRows()
    setKeyText(item.getName())
    addTextRow("[${getString(if (item.isOnline) R.string.online else R.string.offline)}] " + item.ip4)
    setClick {
        DeviceDialog(item).show()
    }
}
