package com.ismartcoding.plain.platform

/**
 * Capabilities the device declares about itself; clients gate UI on these
 * without knowing anything about the device type or OS version. Media/file
 * domains are always present and therefore not listed.
 */
enum class Capability {
    MEDIA_TRASH,
    MIRROR_AUDIO,
    DOC_PREVIEW,
    IMAGE_SEARCH,
    MEDIA_SCAN,
    SMS,
    CALLS,
    CALL_PHONE,
    CONTACTS,
    PACKAGES,
    NOTES,
    FEEDS,
    SCREEN_MIRROR,
    IMAGE_EDITOR,
    NOTIFICATIONS,
    CLIPBOARD,
    POMODORO,
}

/**
 * Capabilities supported by this device right now; platform-specific
 * gating (e.g. Android SDK level) stays inside the actuals.
 */
expect fun getDeviceCapabilities(): List<Capability>
