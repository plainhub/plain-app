package com.ismartcoding.plain.extensions

import com.ismartcoding.plain.features.device.DeviceSortBy
import com.ismartcoding.plain.features.device.getName
import com.ismartcoding.plain.features.file.DFile
import com.ismartcoding.plain.features.file.FileSortBy
import com.ismartcoding.plain.fragment.DeviceFragment

fun List<DeviceFragment>.sorted(sortBy: DeviceSortBy): List<DeviceFragment> {
    return when (sortBy) {
        DeviceSortBy.NAME_ASC -> this.sortedBy { it.getName().lowercase() }
        DeviceSortBy.NAME_DESC -> this.sortedByDescending { it.getName().lowercase() }
        DeviceSortBy.IP_ADDRESS -> this.sortedBy { it.ip4 }
        DeviceSortBy.LAST_ACTIVE -> this.sortedByDescending { it.activeAt }
    }
}

fun List<DFile>.sorted(sortBy: FileSortBy): List<DFile> {
    val comparator = compareBy<DFile> { if (it.isDir) 0 else 1 }
    return when (sortBy) {
        FileSortBy.NAME_ASC -> {
            this.sortedWith(comparator.thenBy { it.name.lowercase() })
        }

        FileSortBy.NAME_DESC -> {
            this.sortedWith(comparator.thenByDescending { it.name.lowercase() })
        }

        FileSortBy.SIZE_ASC -> {
            this.sortedWith(comparator.thenBy { it.size })
        }

        FileSortBy.SIZE_DESC -> {
            this.sortedWith(comparator.thenByDescending { it.size })
        }

        FileSortBy.DATE_ASC -> {
            this.sortedWith(comparator.thenBy { it.updatedAt })
        }

        FileSortBy.DATE_DESC -> {
            this.sortedWith(comparator.thenByDescending { it.updatedAt })
        }
    }
}