package com.ismartcoding.plain.data.enums

import android.content.Context
import android.os.LocaleList
import com.ismartcoding.plain.R
import java.util.*

enum class Language(val value: Int) {
    UseDeviceLanguage(0),
    English(1),
    ChineseSimplified(2);

    fun getText(context: Context): String =
        when (this) {
            UseDeviceLanguage -> context.getString(R.string.use_device_language)
            English -> context.getString(R.string.english)
            ChineseSimplified -> context.getString(R.string.chinese_simplified)
        }

    fun setLocale(context: Context) {
        val locale = getLocale()
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

    private fun getLocale(): Locale =
        when (this) {
            UseDeviceLanguage -> LocaleList.getDefault().get(0)
            English -> Locale("en", "US")
            ChineseSimplified -> Locale("zh", "CN")
        }
}

