package com.ismartcoding.plain.httpserver.loaders

import com.ismartcoding.plain.enums.DriveType
import com.ismartcoding.plain.platform.appDir
import com.ismartcoding.plain.platform.getInternalStorageName
import com.ismartcoding.plain.platform.getInternalStoragePath
import com.ismartcoding.plain.platform.getInternalStorageStats
import com.ismartcoding.plain.platform.getSDCardPath
import com.ismartcoding.plain.platform.getSDCardStorageStats
import com.ismartcoding.plain.platform.getUSBStorageStats
import com.ismartcoding.plain.platform.getUsbDiskPaths
import com.ismartcoding.plain.httpserver.models.StorageMount

object MountsLoader {
    fun load(): List<StorageMount> {
        fun buildMount(
            path: String,
            name: String,
            totalBytes: Long,
            freeBytes: Long,
            driveType: DriveType,
        ): StorageMount {
            return StorageMount(
                id = "path:$path",
                name = name,
                path = path,
                mountPoint = path,
                totalBytes = totalBytes,
                freeBytes = freeBytes,
                usedBytes = (totalBytes - freeBytes).coerceAtLeast(0),
                remote = false,
                alias = "",
                driveType = driveType,
                diskID = "",
            )
        }

        val mounts = mutableListOf<StorageMount>()

        val internalPath = getInternalStoragePath()
        val internalStats = getInternalStorageStats()
        mounts.add(
            buildMount(
                path = internalPath,
                name = getInternalStorageName(),
                totalBytes = internalStats.totalBytes,
                freeBytes = internalStats.freeBytes,
                driveType = DriveType.INTERNAL_STORAGE,
            )
        )

        val sdPath = getSDCardPath()
        if (sdPath.isNotEmpty()) {
            val sdStats = getSDCardStorageStats()
            mounts.add(
                buildMount(
                    path = sdPath,
                    name = "",
                    totalBytes = sdStats.totalBytes,
                    freeBytes = sdStats.freeBytes,
                    driveType = DriveType.SDCARD,
                )
            )
        }

        val usbPaths = getUsbDiskPaths()
        val usbStats = getUSBStorageStats()
        usbPaths.forEachIndexed { index, path ->
            val stats = usbStats.getOrNull(index) ?: return@forEachIndexed
            mounts.add(
                buildMount(
                    path = path,
                    name = "",
                    totalBytes = stats.totalBytes,
                    freeBytes = stats.freeBytes,
                    driveType = DriveType.USB_STORAGE,
                )
            )
        }

        val appPath = appDir()
        if (appPath.isNotEmpty()) {
            mounts.add(
                buildMount(
                    path = appPath,
                    name = "",
                    totalBytes = 0L,
                    freeBytes = 0L,
                    driveType = DriveType.APP,
                )
            )
        }

        return mounts
    }
}
