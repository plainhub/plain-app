package com.ismartcoding.plain.preferences

import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.preferences.core.Preferences
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.DarkTheme
import com.ismartcoding.plain.platform.randomPassword

fun DarkThemePreference.setDarkMode(theme: DarkTheme) {
    when (theme) {
        DarkTheme.ON -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
        DarkTheme.OFF -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
        else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
}

suspend fun DarkThemePreference.putAsync(value: DarkTheme) {
    putAsync(value.value)   // calls the base member putAsync(Int)
    setDarkMode(value)
}

suspend fun AdbTokenPreference.ensureValueAsync(preferences: Preferences) {
    TempData.adbToken = get(preferences)
    if (TempData.adbToken.isEmpty()) {
        TempData.adbToken = randomPassword(32)
        putAsync(TempData.adbToken)
    }
}
