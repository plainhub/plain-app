package com.ismartcoding.plain.platform.system

import com.ismartcoding.plain.platform.permissions.FeaturePermission
import com.ismartcoding.plain.platform.permissions.Permission

enum class AppFeatureType {
    WEB_PORTAL,
    DOCS,
    NOTES,
    FEEDS,
    APPS,
    FILES,
    IMAGES,
    CHAT,
    AUDIO,
    VIDEOS,
    CALLS,
    CONTACTS,
    SMS,
    SOUND_METER,
    POMODORO_TIMER,
    NOTIFICATIONS,
    CHECK_UPDATES,
    MIRROR_AUDIO,
    DONATION,
    DLNA_RECEIVER,
    IMAGE_EDITOR;

    fun getPermission(): FeaturePermission? {
        return when (this) {
            FILES -> {
                FeaturePermission(setOf(Permission.WRITE_EXTERNAL_STORAGE), Permission.WRITE_EXTERNAL_STORAGE)
            }

            CONTACTS -> {
                FeaturePermission(setOf(Permission.READ_CONTACTS, Permission.WRITE_CONTACTS), Permission.READ_CONTACTS)
            }

            SMS -> {
                FeaturePermission(setOf(Permission.READ_SMS), Permission.READ_SMS)
            }

            CALLS -> {
                FeaturePermission(setOf(Permission.READ_CALL_LOG, Permission.WRITE_CALL_LOG), Permission.READ_CALL_LOG)
            }

            else -> {
                null
            }
        }
    }
}
