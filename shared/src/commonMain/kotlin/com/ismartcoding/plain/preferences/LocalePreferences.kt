package com.ismartcoding.plain.preferences

import androidx.datastore.preferences.core.Preferences
import com.ismartcoding.plain.platform.Locale
import com.ismartcoding.plain.platform.setSystemLocale

fun LanguagePreference.getLocale(preferences: Preferences): Locale? {
    return parseLocale(get(preferences))
}

suspend fun LanguagePreference.getLocaleAsync(): Locale? {
    return parseLocale(getAsync())
}

private fun parseLocale(value: String): Locale? {
    if (value.isEmpty()) return null
    val s = value.split("-")
    return if (s.size > 1) Locale(s[0], s[1]) else Locale(value, "")
}

/**
 * Persist the given locale and apply it to the system.
 * Pass null to revert to the device default locale.
 */
suspend fun LanguagePreference.putAsync(locale: Locale?) {
    var value = ""
    if (locale != null) {
        value = locale.language
        if (locale.country.isNotEmpty()) value += "-${locale.country}"
    }
    putAsync(value)
    setSystemLocale(locale)
}
