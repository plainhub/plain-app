package com.ismartcoding.plain.enums

import android.content.Context
import com.ismartcoding.lib.isRPlus
import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.data.DFeaturePermission
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.features.Permissions

enum class AppFeatureType {
    DOCS,
    NOTES,
    FEEDS,
    APPS,
    FILES,
    CALLS,
    CONTACTS,
    SMS,
    SOUND_METER,
    POMODORO_TIMER,
    NOTIFICATIONS,
    CHECK_UPDATES,
    MEDIA_TRASH;

    fun has(): Boolean {
        return when (this) {
            APPS, SMS, CALLS, NOTIFICATIONS -> {
                BuildConfig.CHANNEL != AppChannelType.GOOGLE.name
            }

            MEDIA_TRASH -> {
                isRPlus() // Android 11+
            }

            CHECK_UPDATES -> {
                BuildConfig.CHANNEL == AppChannelType.GITHUB.name
            }

            else -> true
        }
    }

    fun hasPermission(context: Context): Boolean {
        val p = getPermission()
        if (p != null) {
            return Permissions.allCan(context, p.permissions)
        }

        return true
    }

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