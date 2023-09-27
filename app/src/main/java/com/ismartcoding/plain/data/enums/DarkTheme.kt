package com.ismartcoding.plain.data.enums

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import com.ismartcoding.plain.R

enum class DarkTheme(val value: Int) {
    UseDeviceTheme(0),
    ON(1),
    OFF(2),
    ;

    fun getText(context: Context): String =
        when (this) {
            UseDeviceTheme -> context.getString(R.string.use_device_theme)
            ON -> context.getString(R.string.on)
            OFF -> context.getString(R.string.off)
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
            return DarkTheme.values().find { it.value == value } ?: UseDeviceTheme
        }
    }
}
