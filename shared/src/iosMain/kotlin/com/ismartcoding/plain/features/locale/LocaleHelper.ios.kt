package com.ismartcoding.plain.features.locale

import platform.Foundation.NSLocale
import platform.Foundation.currentLocale

actual fun currentLocale(): Locale {
    val ns = NSLocale.currentLocale
    return Locale(
        language = ns.languageCode ?: "en",
        country = ns.countryCode ?: "US",
    )
}
