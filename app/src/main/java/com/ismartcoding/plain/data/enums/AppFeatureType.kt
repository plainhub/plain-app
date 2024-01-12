package com.ismartcoding.plain.data.enums

import com.ismartcoding.plain.BuildConfig

enum class AppFeatureType {
    SOCIAL,
    EDUCATION,
    HARDWARE,
    EXCHANGE_RATE;

    fun has(): Boolean {
        when {
            this == SOCIAL -> {
                return BuildConfig.CHANNEL != AppChannelType.GOOGLE.name
            }
            this == EXCHANGE_RATE -> {
                return BuildConfig.CHANNEL != AppChannelType.CHINA.name
            }
            this == EDUCATION -> {
                return BuildConfig.DEBUG
            }
            this == HARDWARE -> {
                return BuildConfig.DEBUG
            }
            else -> return true
        }
    }
}