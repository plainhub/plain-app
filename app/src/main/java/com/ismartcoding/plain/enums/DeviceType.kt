package com.ismartcoding.plain.enums

import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper.getString

enum class DeviceType(val value: String) {
    COMPUTER("computer"),
    PHONE("phone"),
    TABLET("tablet"),
    TV("tv"),
    OTHER("other");

    fun getText(): String {
        return when (this) {
            COMPUTER -> getString(R.string.computer)
            PHONE -> getString(R.string.phone)
            TABLET -> getString(R.string.tablet)
            TV -> getString(R.string.tv)
            OTHER -> getString(R.string.other)
        }
    }

    fun getIcon(): Int {
        return when (this) {
            COMPUTER -> R.drawable.laptop
            PHONE -> R.drawable.smartphone
            TABLET -> R.drawable.tablet
            TV -> R.drawable.tv
            OTHER -> R.drawable.devices
        }
    }

    companion object {
        fun fromValue(value: String): DeviceType {
            return entries.find { it.value == value } ?: OTHER
        }
    }
}