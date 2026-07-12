package com.ismartcoding.plain.enums

import com.ismartcoding.plain.data.DFeaturePermission
import com.ismartcoding.plain.platform.Permission

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
    MEDIA_TRASH,
    DONATION,
    DLNA_RECEIVER;

    fun getPermission(): DFeaturePermission? {
        return when (this) {
            FILES -> {
                DFeaturePermission(setOf(Permission.WRITE_EXTERNAL_STORAGE), Permission.WRITE_EXTERNAL_STORAGE)
            }

            CONTACTS -> {
                DFeaturePermission(setOf(Permission.READ_CONTACTS, Permission.WRITE_CONTACTS), Permission.READ_CONTACTS)
            }

            SMS -> {
                DFeaturePermission(setOf(Permission.READ_SMS), Permission.READ_SMS)
            }

            CALLS -> {
                DFeaturePermission(setOf(Permission.READ_CALL_LOG, Permission.WRITE_CALL_LOG), Permission.READ_CALL_LOG)
            }

            else -> {
                null
            }
        }
    }
}
