package com.ismartcoding.plain.platform.files

import com.ismartcoding.plain.appContext

actual fun getInternalStoragePath(): String = FileSystemHelper.getInternalStoragePath()

actual fun getInternalStorageName(): String = FileSystemHelper.getInternalStorageName()

actual fun getSDCardPath(): String = FileSystemHelper.getSDCardPath(appContext)

actual fun getUsbDiskPaths(): List<String> = FileSystemHelper.getUsbDiskPaths()

actual fun getInternalStorageStats(): StorageStats = FileSystemHelper.getInternalStorageStats()

actual fun getSDCardStorageStats(): StorageStats = FileSystemHelper.getSDCardStorageStats(appContext)

actual fun getUSBStorageStats(): List<StorageStats> = FileSystemHelper.getUSBStorageStats()

internal actual fun grantedStorageDirectories(): List<String> = emptyList()
