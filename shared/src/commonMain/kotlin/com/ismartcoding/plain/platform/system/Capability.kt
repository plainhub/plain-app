package com.ismartcoding.plain.platform.system

enum class Capability {
    ADB,
    DIRECTORY_ACCESS,
    BLE,
    WIFI_AWARE,
    BACKGROUND_SERVICE,
    SOUND_METER,

    BACKGROUND_TIMER,
    BACKGROUND_WAKE,
    FILE_EXPORT,
    FILE_REVEAL,
    FILE_CLIPBOARD,
    APP_RELAUNCH,
    MEDIA_TRASH,
    MEDIA_DELETE,
    MEDIA_MOVE,
    MEDIA_FAVORITE,
    VIDEO_REMUX,
    VIDEO_TRANSCODE,
    APP_UPDATES,

    MIRROR_AUDIO,
    DOC_PREVIEW,
    IMAGE_SEARCH,
    MEDIA_SCAN,
    TELEPHONY,
    PACKAGES,
    SCREEN_MIRROR,
    IMAGE_EDITOR,
    NOTIFICATIONS,
    CLIPBOARD,
    LAN_SHARE,
    DISK_MANAGER,
}

/**
 * Capabilities supported by this device right now; platform-specific
 * gating (e.g. Android SDK level) stays inside the actuals.
 */
expect fun getDeviceCapabilities(): List<Capability>
