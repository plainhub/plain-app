package com.ismartcoding.plain.platform.files


/**
 * Path to the primary internal storage (e.g. /storage/emulated/0).
 * Empty string on iOS.
 */
expect fun getInternalStoragePath(): String

/**
 * Display name for internal storage.
 */
expect fun getInternalStorageName(): String

/**
 * Path to the SD card, or empty string if none.
 */
expect fun getSDCardPath(): String

/**
 * Paths to USB storage devices.
 */
expect fun getUsbDiskPaths(): List<String>

/**
 * Storage stats for internal storage.
 */
expect fun getInternalStorageStats(): StorageStats

/**
 * Storage stats for the SD card.
 */
expect fun getSDCardStorageStats(): StorageStats

/**
 * Storage stats for each USB storage device.
 */
expect fun getUSBStorageStats(): List<StorageStats>

internal expect fun grantedStorageDirectories(): List<String>
