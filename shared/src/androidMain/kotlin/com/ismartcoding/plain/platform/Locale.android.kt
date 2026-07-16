package com.ismartcoding.plain.platform

import android.content.Context
import android.os.LocaleList as AndroidLocaleList
import com.ismartcoding.plain.appContext

private var appContextValue: Context? = null

fun setLocaleContext(context: Context) {
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

actual fun getLocaleDisplayName(locale: Locale): String {
    val android = java.util.Locale(locale.language, locale.country)
    return android.getDisplayName(android)
}

/**
 * Apply the given locale to Android resources + JVM default.
 * Mirrors the previous `Language.setLocale(context, locale)` behavior.
 */
actual fun setSystemLocale(locale: Locale?) {
    val ctx = appContext ?: return
    val androidLocale = locale?.let { java.util.Locale(it.language, it.country) }
        ?: AndroidLocaleList.getDefault().get(0)
    java.util.Locale.setDefault(androidLocale)
    val localeList = AndroidLocaleList(androidLocale)
    AndroidLocaleList.setDefault(localeList)

    val resources = ctx.resources
    val metrics = resources.displayMetrics
    val configuration = resources.configuration
    configuration.setLocale(androidLocale)
    configuration.setLocales(localeList)
    ctx.createConfigurationContext(configuration)
    resources.updateConfiguration(configuration, metrics)

    val appResources = ctx.applicationContext.resources
    val appMetrics = appResources.displayMetrics
    val appConfiguration = appResources.configuration
    appConfiguration.setLocale(androidLocale)
    appConfiguration.setLocales(localeList)
    ctx.applicationContext.createConfigurationContext(appConfiguration)
    appResources.updateConfiguration(appConfiguration, appMetrics)
}
