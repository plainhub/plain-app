package com.ismartcoding.plain

import android.app.usage.StorageStatsManager
import android.os.storage.StorageManager
import com.ismartcoding.plain.lib.extensions.getSystemServiceCompat

val storageManager: StorageManager by lazy {
    appContext.getSystemServiceCompat(StorageManager::class.java)
}

val storageStatsManager: StorageStatsManager by lazy {
    appContext.getSystemServiceCompat(StorageStatsManager::class.java)
}
