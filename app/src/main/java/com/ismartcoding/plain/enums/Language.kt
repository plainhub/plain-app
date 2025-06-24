package com.ismartcoding.plain.enums

import android.content.Context
import android.os.LocaleList
import com.ismartcoding.plain.preferences.LanguagePreference
import java.util.*

object Language {
    val locales =
        listOf(
            Locale("en", "US"),
            Locale("zh", "CN"),
            Locale("zh", "TW"),
            Locale("es", ""),
            Locale("ja", ""),
            Locale("nl", ""),
            Locale("it", ""),
            Locale("hi", ""),
            Locale("fr", ""),
            Locale("ru", ""),
            Locale("bn", ""),
            Locale("de", ""),
            Locale("pt", ""),
            Locale("ta", ""),
            Locale("ko", ""),
            Locale("tr", ""),
            Locale("vi", ""),
        )

    fun setLocale(
        context: Context,
        locale: Locale,
    ) {
        val resources = context.resources
        val metrics = resources.displayMetrics
        val configuration = resources.configuration
        configuration.setLocale(locale)
        configuration.setLocales(LocaleList(locale))
        context.createConfigurationContext(configuration)
        resources.updateConfiguration(configuration, metrics)

        val appResources = context.applicationContext.resources
        val appMetrics = appResources.displayMetrics
        val appConfiguration = appResources.configuration
        appConfiguration.setLocale(locale)
        appConfiguration.setLocales(LocaleList(locale))
        context.applicationContext.createConfigurationContext(appConfiguration)
        appResources.updateConfiguration(appConfiguration, appMetrics)
    }

    suspend fun initLocaleAsync(context: Context) {
        val locale = LanguagePreference.getLocaleAsync(context)
        if (locale != null) {
            setLocale(context, locale)
        }
    }
}
