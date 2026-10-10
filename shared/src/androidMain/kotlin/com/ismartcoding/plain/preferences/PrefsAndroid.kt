package com.ismartcoding.plain.preferences

import androidx.appcompat.app.AppCompatDelegate
import com.ismartcoding.plain.enums.DarkTheme

fun setDarkMode(theme: DarkTheme) {
    AppCompatDelegate.setDefaultNightMode(when (theme) {
        DarkTheme.ON -> AppCompatDelegate.MODE_NIGHT_YES
        DarkTheme.OFF -> AppCompatDelegate.MODE_NIGHT_NO
        else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    })
}
