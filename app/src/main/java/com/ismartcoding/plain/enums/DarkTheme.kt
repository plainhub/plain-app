package com.ismartcoding.plain.enums

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.stringResource

enum class DarkTheme(val value: Int) {
    UseDeviceTheme(0),
    ON(1),
    OFF(2),
    ;

    @Composable
    fun getText(): String =
        when (this) {
            UseDeviceTheme -> stringResource(Res.string.use_device_theme)
            ON -> stringResource(Res.string.on)
            OFF -> stringResource(Res.string.off)
        }

    companion object {
        @Composable
        @ReadOnlyComposable
        fun isDarkTheme(value: Int): Boolean =
            when (value) {
                UseDeviceTheme.value -> isSystemInDarkTheme()
                ON.value -> true
                OFF.value -> false
                else -> isSystemInDarkTheme()
            }

        fun parse(value: Int): DarkTheme {
            return entries.find { it.value == value } ?: UseDeviceTheme
        }
    }
}
