package com.ismartcoding.plain.enums

import com.ismartcoding.plain.buildChannel
import com.ismartcoding.plain.features.allGranted
import com.ismartcoding.plain.platform.isAndroidOnly
import com.ismartcoding.plain.platform.isDebugBuild
import com.ismartcoding.plain.platform.isHuawei
import com.ismartcoding.plain.platform.isQPlus
import com.ismartcoding.plain.platform.isRPlus

fun AppFeatureType.has(): Boolean {
    return when (this) {
        AppFeatureType.APPS, AppFeatureType.SMS, AppFeatureType.CALLS, AppFeatureType.NOTIFICATIONS, AppFeatureType.DONATION -> {
            isAndroidOnly() &&  buildChannel != AppChannelType.GOOGLE.name
        }

        AppFeatureType.MIRROR_AUDIO -> {
            isQPlus()
        }

        AppFeatureType.MEDIA_TRASH -> {
            isRPlus()
        }

        AppFeatureType.CHECK_UPDATES -> {
            buildChannel == AppChannelType.GITHUB.name
        }

        AppFeatureType.IMAGE_EDITOR -> {
            isDebugBuild()
        }

        AppFeatureType.MEDIA_FAVORITE -> {
            isQPlus() && !isHuawei()
        }

        else -> true
    }
}

fun AppFeatureType.hasPermission(): Boolean {
    val p = getPermission()
    if (p != null) {
        return allGranted(p.permissions)
    }

    return true
}
