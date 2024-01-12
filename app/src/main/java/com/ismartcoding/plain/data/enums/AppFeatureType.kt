package com.ismartcoding.plain.data.enums

import com.ismartcoding.plain.BuildConfig

enum class AppFeatureType {
    SOCIAL,
    EDUCATION,
    HARDWARE,
    EXCHANGE_RATE;

    fun has(): Boolean {
        when(this) {
            SOCIAL -> {
                return BuildConfig.CHANNEL != AppChannelType.GOOGLE.name
            }
            EXCHANGE_RATE -> {
                return BuildConfig.CHANNEL != AppChannelType.CHINA.name
            }
            EDUCATION, HARDWARE -> {
                return BuildConfig.DEBUG
            }
            else -> return true
        }
    }
}