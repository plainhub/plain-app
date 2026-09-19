package com.ismartcoding.plain.platform

/**
 * Optional capabilities the device declares about itself; clients gate UI on
 * these without knowing anything about the underlying OS version.
 */
enum class DeviceFeature {
    MEDIA_TRASH,
    MIRROR_AUDIO,
}

/**
 * Capabilities supported by this device right now; platform-specific
 * gating (e.g. Android SDK level) stays inside the actuals.
 */
expect fun getDeviceFeatures(): List<DeviceFeature>
