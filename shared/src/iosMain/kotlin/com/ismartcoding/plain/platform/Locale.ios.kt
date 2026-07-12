package com.ismartcoding.plain.platform

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSLocale
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
