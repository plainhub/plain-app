package com.ismartcoding.plain.enums

import com.ismartcoding.plain.buildChannel
import com.ismartcoding.plain.platform.isQPlus
import com.ismartcoding.plain.platform.isRPlus

fun AppFeatureType.has(): Boolean {
    return when (this) {
        AppFeatureType.APPS, AppFeatureType.SMS, AppFeatureType.CALLS, AppFeatureType.NOTIFICATIONS, AppFeatureType.DONATION -> {
            buildChannel != AppChannelType.GOOGLE.name
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

        else -> true
    }
}
