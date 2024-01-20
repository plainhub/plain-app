package com.ismartcoding.plain.data.enums

import com.ismartcoding.plain.BuildConfig

enum class AppFeatureType {
    SOCIAL,
    EDUCATION,
    HARDWARE,
    APPS,
    NOTIFICATIONS,
    EXCHANGE_RATE;

    fun has(): Boolean {
        when(this) {
            SOCIAL, NOTIFICATIONS, APPS -> {
                return BuildConfig.CHANNEL != AppChannelType.GOOGLE.name
            }
            EXCHANGE_RATE, EDUCATION, HARDWARE -> {
                return BuildConfig.DEBUG
            }
            else -> return true
        }
    }
}