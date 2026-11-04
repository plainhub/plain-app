package com.ismartcoding.plain.platform.system

enum class Capability {
    BLE,
    WIFI_AWARE,
    BACKGROUND_SERVICE,
    LAUNCHER_SHORTCUTS,
    FILE_BROWSER,
    IMAGE_LIBRARY,
    AUDIO_LIBRARY,
    VIDEO_LIBRARY,
    DOCUMENT_LIBRARY,
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
    VIDEO_3GP_CONVERT,
    APP_UPDATES,
    DONATION,

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
    LAN_SHARE,
    DISK_MANAGER,
}

/**
 * Capabilities supported by this device right now; platform-specific
 * gating (e.g. Android SDK level) stays inside the actuals.
 */
expect fun getDeviceCapabilities(): List<Capability>
