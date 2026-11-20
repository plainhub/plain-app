package com.ismartcoding.plain.platform.files

import android.os.Environment
import android.os.StatFs
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.internal_storage
import com.ismartcoding.plain.platform.isRPlus
import com.ismartcoding.plain.storageManager
import com.ismartcoding.plain.ui.i18n.LocaleHelper

actual fun getInternalStoragePath(): String {
    if (isRPlus()) storageManager.primaryStorageVolume.directory?.let { return it.path }
    return Environment.getExternalStorageDirectory().absolutePath
}

actual fun getInternalStorageName(): String = LocaleHelper.getString(Res.string.internal_storage)

internal actual fun storageStats(path: String): StorageStats? = try {
    val stat = StatFs(path)
    StorageStats(stat.totalBytes, stat.availableBytes)
} catch (_: IllegalArgumentException) {
    // The volume may have been unmounted since enumeration.
    null
}

internal actual fun grantedStorageDirectories(): List<String> = emptyList()
