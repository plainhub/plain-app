package com.ismartcoding.plain.web.loaders

import android.content.Context
import com.ismartcoding.plain.features.file.FileSystemHelper
import com.ismartcoding.plain.web.models.StorageMount

object MountsLoader {
    fun load(context: Context): List<StorageMount> {
        fun buildMount(
            path: String,
            name: String,
            totalBytes: Long,
            freeBytes: Long,
            driveType: String,
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

        val internalPath = FileSystemHelper.getInternalStoragePath()
        val internalStats = FileSystemHelper.getInternalStorageStats()
        mounts.add(
            buildMount(
                path = internalPath,
                name = FileSystemHelper.getInternalStorageName(),
                totalBytes = internalStats.totalBytes,
                freeBytes = internalStats.freeBytes,
                driveType = "INTERNAL_STORAGE",
            )
        )

        val sdPath = FileSystemHelper.getSDCardPath(context)
        if (sdPath.isNotEmpty()) {
            val sdStats = FileSystemHelper.getSDCardStorageStats(context)
            mounts.add(
                buildMount(
                    path = sdPath,
                    name = "",
                    totalBytes = sdStats.totalBytes,
                    freeBytes = sdStats.freeBytes,
                    driveType = "SDCARD",
                )
            )
        }

        val usbPaths = FileSystemHelper.getUsbDiskPaths()
        val usbStats = FileSystemHelper.getUSBStorageStats()
        usbPaths.forEachIndexed { index, path ->
            val stats = usbStats.getOrNull(index) ?: return@forEachIndexed
            mounts.add(
                buildMount(
                    path = path,
                    name = "",
                    totalBytes = stats.totalBytes,
                    freeBytes = stats.freeBytes,
                    driveType = "USB_STORAGE",
                )
            )
        }

        val appPath = FileSystemHelper.getExternalFilesDirPath(context)
        if (appPath.isNotEmpty()) {
            mounts.add(
                buildMount(
                    path = appPath,
                    name = "",
                    totalBytes = 0L,
                    freeBytes = 0L,
                    driveType = "APP",
                )
            )
        }

        return mounts
    }
}
