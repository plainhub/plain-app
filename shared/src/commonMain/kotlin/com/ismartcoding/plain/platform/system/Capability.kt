package com.ismartcoding.plain.platform.system

enum class Capability {
    ADB,
    BLE,
    WIFI_AWARE,

    APP_RELAUNCH,
    MEDIA_TRASH,
    MEDIA_DELETE,
    MEDIA_MOVE,
    MEDIA_FAVORITE,
    VIDEO_REMUX,
    VIDEO_TRANSCODE,
    APP_UPDATES,

    MIRROR_AUDIO,
    MEDIA_SCAN,
    TELEPHONY,
    PACKAGES,
    SCREEN_MIRROR,
    NOTIFICATIONS,
    CLIPBOARD,
    SMB_SERVER,
    DISK_MANAGER,
}

/**
 * Capabilities supported by this device right now; platform-specific
 * gating (e.g. Android SDK level) stays inside the actuals.
 */
expect fun getDeviceCapabilities(): List<Capability>
