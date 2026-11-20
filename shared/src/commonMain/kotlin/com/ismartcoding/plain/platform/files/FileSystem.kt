package com.ismartcoding.plain.platform.files

import com.ismartcoding.plain.features.files.DriveType

internal data class StorageRoot(val path: String, val type: DriveType)

expect fun getInternalStoragePath(): String
expect fun getInternalStorageName(): String

internal expect fun storageRoots(): List<StorageRoot>
internal expect fun storageStats(path: String): StorageStats?
internal expect fun grantedStorageDirectories(): List<String>
