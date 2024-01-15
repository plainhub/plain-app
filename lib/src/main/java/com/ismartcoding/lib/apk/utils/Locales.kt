package com.ismartcoding.lib.apk.utils

import java.util.Locale

object Locales {
    /**
     * when do localize, any locale will match this
     */
    val any = Locale("", "")

    /**
     * How much the given locale match the expected locale.
     */
    fun match(locale: Locale?, targetLocale: Locale?): Int {
        if (locale == null) {
            return -1
        }
        return if (locale.language == targetLocale!!.language) {
            if (locale.country == targetLocale.country) {
                3
            } else if (targetLocale.country.isEmpty()) {
                2
            } else {
                0
            }
        } else if (targetLocale.country.isEmpty() || targetLocale.language.isEmpty()) {
            1
        } else {
            0
        }
    }
}