package com.ismartcoding.plain.platform.files

import com.ismartcoding.plain.features.files.DriveType
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.app_data
import com.ismartcoding.plain.i18n.sdcard
import com.ismartcoding.plain.i18n.usb_storage
import com.ismartcoding.plain.platform.system.appDir
import com.ismartcoding.plain.rust.host.files.MountFacts
import com.ismartcoding.plain.ui.i18n.LocaleHelper

internal fun storageVolumeFacts(
    roots: () -> List<StorageRoot> = ::storageRoots,
    statsForPath: (String) -> StorageStats? = ::storageStats,
    grants: List<String> = grantedStorageDirectories(),
    appPath: String = appDir(),
    name: (DriveType) -> String = { type ->
        when (type) {
            DriveType.INTERNAL_STORAGE -> getInternalStorageName()
            DriveType.SDCARD -> LocaleHelper.getString(Res.string.sdcard)
            DriveType.USB_STORAGE -> LocaleHelper.getString(Res.string.usb_storage)
            DriveType.APP -> LocaleHelper.getString(Res.string.app_data)
        }
    },
): List<MountFacts> = buildList {
    var usbCount = 0
    for ((path, type) in roots()) {
        val stats = statsForPath(path) ?: continue
        val label = if (type == DriveType.USB_STORAGE) "${name(type)} ${++usbCount}" else name(type)
        add(MountFacts(label, path, stats.totalBytes, stats.freeBytes, type))
    }
    for (path in grants) {
        add(MountFacts(path.substringAfterLast('/'), path, 0, 0, DriveType.INTERNAL_STORAGE))
    }
    if (appPath.isNotEmpty()) add(MountFacts(name(DriveType.APP), appPath, 0, 0, DriveType.APP))
}
