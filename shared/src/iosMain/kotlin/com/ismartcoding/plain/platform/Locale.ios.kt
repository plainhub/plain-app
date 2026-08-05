package com.ismartcoding.plain.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.*

@OptIn(ExperimentalForeignApi::class)
actual fun currentLocale(): Locale {
    val preferred = NSLocale.preferredLanguages.firstOrNull() as? String
    val identifier = preferred ?: NSLocale.currentLocale.localeIdentifier
    val nsLocale = NSLocale(localeIdentifier = identifier)
    return Locale(
        language = nsLocale.languageCode,
        country = nsLocale.countryCode.orEmpty(),
    )
}

@OptIn(ExperimentalForeignApi::class)
actual fun getLocaleDisplayName(locale: Locale): String {
    val identifier = if (locale.country.isNotEmpty()) "${locale.language}_${locale.country}" else locale.language
    return NSLocale.currentLocale.displayNameForKey(NSLocaleIdentifier, identifier) ?: identifier
}

@OptIn(ExperimentalForeignApi::class)
actual fun setSystemLocale(locale: Locale?) {
    val defaults = NSUserDefaults.standardUserDefaults
    if (locale != null) {
        val identifier = if (locale.country.isNotEmpty()) {
            "${locale.language}-${locale.country}"
        } else {
            locale.language
        }
        defaults.setObject(listOf(identifier), forKey = "AppleLanguages")
    } else {
        defaults.removeObjectForKey("AppleLanguages")
    }
    defaults.synchronize()
}
