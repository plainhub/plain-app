package com.ismartcoding.plain.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSLocale
import platform.Foundation.NSLocaleIdentifier
import platform.Foundation.currentLocale
import platform.Foundation.localeIdentifier

@OptIn(ExperimentalForeignApi::class)
actual fun currentLocale(): Locale {
    val ns = NSLocale.currentLocale
    val identifier = ns.localeIdentifier
    val parts = identifier.split("_")
    return Locale(
        language = parts.getOrNull(0) ?: "en",
        country = parts.getOrNull(1) ?: "US",
    )
}

@OptIn(ExperimentalForeignApi::class)
actual fun getLocaleDisplayName(locale: Locale): String {
    val identifier = if (locale.country.isNotEmpty()) "${locale.language}_${locale.country}" else locale.language
    return NSLocale.currentLocale.displayNameForKey(NSLocaleIdentifier, identifier) ?: identifier
}

actual fun setSystemLocale(locale: Locale?) {}
