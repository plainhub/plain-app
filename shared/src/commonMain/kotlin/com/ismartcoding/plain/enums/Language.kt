package com.ismartcoding.plain.enums

import com.ismartcoding.plain.platform.Locale
import com.ismartcoding.plain.platform.setSystemLocale
import com.ismartcoding.plain.preferences.LanguagePreference
import com.ismartcoding.plain.preferences.getLocaleAsync

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

    suspend fun initLocaleAsync() {
        val locale = LanguagePreference.getLocaleAsync()
        if (locale != null) {
            setSystemLocale(locale)
        }
    }
}
