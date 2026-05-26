package com.ismartcoding.plain.enums
import com.ismartcoding.plain.preferences.*

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
        // Compose Multiplatform Resources (used after KMP migration) resolves strings
        // via the JVM/process default locale, not via Resources.configuration. We must
        // update both so XML resources AND CMR strings follow the user's choice.
        Locale.setDefault(locale)
        val localeList = LocaleList(locale)
        LocaleList.setDefault(localeList)

        val resources = context.resources
        val metrics = resources.displayMetrics
        val configuration = resources.configuration
        configuration.setLocale(locale)
        configuration.setLocales(localeList)
        context.createConfigurationContext(configuration)
        resources.updateConfiguration(configuration, metrics)

        val appResources = context.applicationContext.resources
        val appMetrics = appResources.displayMetrics
        val appConfiguration = appResources.configuration
        appConfiguration.setLocale(locale)
        appConfiguration.setLocales(localeList)
        context.applicationContext.createConfigurationContext(appConfiguration)
        appResources.updateConfiguration(appConfiguration, appMetrics)
    }

    suspend fun initLocaleAsync(context: Context) {
        val locale = LanguagePreference.getLocaleAsync()
        if (locale != null) {
            setLocale(context, locale)
        }
    }
}
