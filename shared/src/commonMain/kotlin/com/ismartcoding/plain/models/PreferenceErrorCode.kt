package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
enum class PreferenceErrorCode {
    INVALID_PREF_VALUE,
    PREFERENCE_UPDATE_FAILED;

    val wire: String get() = name

    companion object {
        fun fromWire(value: String?): PreferenceErrorCode? = entries.firstOrNull { it.name == value }
    }
}
