package com.ismartcoding.plain.preferences

import androidx.appcompat.app.AppCompatDelegate
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.platform.randomPassword

fun SystemPrefs.setDarkMode(theme: DarkTheme) {
    when (theme) {
        DarkTheme.ON -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        DarkTheme.OFF -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}

fun SystemPrefs.ensureAdbToken() {
    TempData.adbToken = SystemPrefs.adbToken.value
    if (TempData.adbToken.isEmpty()) {
        TempData.adbToken = randomPassword(32)
        adbToken.value = TempData.adbToken
    }
}
