package com.ismartcoding.plain.platform.system

import com.ismartcoding.plain.platform.permissions.FeaturePermission
import com.ismartcoding.plain.platform.permissions.Permission

enum class AppFeatureType {
    DOCS,
    NOTES,
    FEEDS,
    APPS,
    FILES,
    IMAGES,
    AUDIO,
    VIDEOS,
    SOUND_METER,
    POMODORO_TIMER,
    NOTIFICATIONS,
    CHECK_UPDATES,
    IMAGE_EDITOR;

    fun getPermission(): FeaturePermission? = when (this) {
        FILES -> FeaturePermission(setOf(Permission.WRITE_EXTERNAL_STORAGE), Permission.WRITE_EXTERNAL_STORAGE)
        else -> null
    }
}
