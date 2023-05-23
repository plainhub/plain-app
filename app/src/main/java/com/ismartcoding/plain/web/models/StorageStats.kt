package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.features.file.DStorageStatsItem

data class StorageStatsItem(val totalBytes: Long = 0L, val freeBytes: Long = 0L)
data class StorageStats(val internal: StorageStatsItem, val sdcard: StorageStatsItem? = null)
fun DStorageStatsItem.toModel(): StorageStatsItem {
    return StorageStatsItem(totalBytes, freeBytes)
}