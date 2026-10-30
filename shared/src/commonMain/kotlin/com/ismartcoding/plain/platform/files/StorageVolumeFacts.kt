package com.ismartcoding.plain.platform.files

import com.ismartcoding.plain.platform.system.appDir

import com.ismartcoding.plain.features.files.DriveType
import com.ismartcoding.plain.i18n.Res
import com.ismartcoding.plain.i18n.app_data
import com.ismartcoding.plain.i18n.sdcard
import com.ismartcoding.plain.i18n.usb_storage
import com.ismartcoding.plain.rust.host.files.MountFacts
import com.ismartcoding.plain.ui.i18n.LocaleHelper

internal fun storageVolumeFacts(): List<MountFacts> = buildList {
    val internal = getInternalStorageStats()
    add(MountFacts(getInternalStorageName(), getInternalStoragePath(), internal.totalBytes, internal.freeBytes, DriveType.INTERNAL_STORAGE))
    val sdPath = getSDCardPath()
    if (sdPath.isNotEmpty()) {
        val stats = getSDCardStorageStats()
        add(MountFacts(LocaleHelper.getString(Res.string.sdcard), sdPath, stats.totalBytes, stats.freeBytes, DriveType.SDCARD))
    }
    val usbStats = getUSBStorageStats()
    getUsbDiskPaths().forEachIndexed { index, path ->
        usbStats.getOrNull(index)?.let { stats -> add(MountFacts("${LocaleHelper.getString(Res.string.usb_storage)} ${index + 1}", path, stats.totalBytes, stats.freeBytes, DriveType.USB_STORAGE)) }
    }
    if (appDir().isNotEmpty()) add(MountFacts(LocaleHelper.getString(Res.string.app_data), appDir(), 0, 0, DriveType.APP))
}
