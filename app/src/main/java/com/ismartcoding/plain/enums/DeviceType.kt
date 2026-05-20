package com.ismartcoding.plain.enums

import org.jetbrains.compose.resources.DrawableResource
import com.ismartcoding.plain.i18n.*
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

    fun getIcon(): DrawableResource {
        return when (this) {
            COMPUTER -> Res.drawable.laptop
            PHONE -> Res.drawable.smartphone
            TABLET -> Res.drawable.tablet
            TV -> Res.drawable.tv
            OTHER -> Res.drawable.devices
        }
    }

    companion object {
        fun fromValue(value: String): DeviceType {
            return entries.find { it.value == value } ?: OTHER
        }
    }
}