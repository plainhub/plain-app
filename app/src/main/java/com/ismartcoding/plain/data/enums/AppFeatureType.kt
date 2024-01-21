package com.ismartcoding.plain.data.enums

import com.ismartcoding.plain.BuildConfig
import com.ismartcoding.plain.TempData

enum class AppFeatureType {
    SOCIAL,
    EDUCATION,
    HARDWARE,
    APPS,
    NOTIFICATIONS,
    EXCHANGE_RATE;

    fun has(): Boolean {
        when (this) {
            SOCIAL, NOTIFICATIONS -> {
                return BuildConfig.CHANNEL != AppChannelType.GOOGLE.name && !TempData.demoMode
            }

            APPS -> {
                return BuildConfig.CHANNEL != AppChannelType.GOOGLE.name
            }

            EXCHANGE_RATE, EDUCATION, HARDWARE -> {
                return BuildConfig.DEBUG && !TempData.demoMode
            }

            else -> return true
        }
    }
}