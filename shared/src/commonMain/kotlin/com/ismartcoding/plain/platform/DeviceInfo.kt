package com.ismartcoding.plain.platform

import com.ismartcoding.plain.data.DDeviceInfo

/**
 * Returns the device info (manufacturer, model, OS, etc.).
 * Platform-specific fields (e.g. DAndroidDeviceInfo) are populated only on the
 * matching platform; null on others.
 */
expect fun getDeviceInfo(): DDeviceInfo
