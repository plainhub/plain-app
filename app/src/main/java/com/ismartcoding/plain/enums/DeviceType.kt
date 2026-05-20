package com.ismartcoding.plain.enums

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.i18n.*

enum class DeviceType(val value: String) {
    COMPUTER("computer"),
    PHONE("phone"),
    TABLET("tablet"),
    TV("tv"),
    OTHER("other");

    @Composable
    fun getText(): String {
        return when (this) {
            COMPUTER -> stringResource(Res.string.computer)
            PHONE -> stringResource(Res.string.phone)
            TABLET -> stringResource(Res.string.tablet)
            TV -> stringResource(Res.string.tv)
            OTHER -> stringResource(Res.string.other)
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