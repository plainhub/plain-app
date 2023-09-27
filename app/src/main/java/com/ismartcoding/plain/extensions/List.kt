package com.ismartcoding.plain.extensions

import com.ismartcoding.plain.features.device.DeviceSortBy
import com.ismartcoding.plain.features.device.getName
import com.ismartcoding.plain.fragment.DeviceFragment

fun List<DeviceFragment>.sorted(sortBy: DeviceSortBy): List<DeviceFragment> {
    return when (sortBy) {
        DeviceSortBy.NAME_ASC -> this.sortedBy { it.getName().lowercase() }
        DeviceSortBy.NAME_DESC -> this.sortedByDescending { it.getName().lowercase() }
        DeviceSortBy.IP_ADDRESS -> this.sortedBy { it.ip4 }
        DeviceSortBy.LAST_ACTIVE -> this.sortedByDescending { it.activeAt }
    }
}
