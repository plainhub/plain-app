package com.ismartcoding.plain.features.locale

import android.content.Context

private var appContextValue: Context? = null

fun setAppContext(context: Context) {
    appContextValue = context
}

actual fun currentLocale(): Locale {
    val ctx = appContextValue ?: return Locale("en", "US")
    val cfg = ctx.resources.configuration
    val androidLocale = cfg.locales.get(0)
    return Locale(
        language = androidLocale.language,
        country = androidLocale.country,
    )
}
