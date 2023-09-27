package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper

object FormatHelper {
    fun formatSeconds(n: Int): String {
        val seconds = n % 60
        val minutes = n / 60 % 60
        val hours = n / 3600
        var r = ""
        if (hours > 0) {
            r += LocaleHelper.getQuantityString(R.plurals.n_hours, hours)
        }

        if (minutes > 0) {
            r += LocaleHelper.getQuantityString(R.plurals.n_minutes, minutes)
        }

        if (seconds > 0) {
            r += LocaleHelper.getQuantityString(R.plurals.n_seconds, seconds)
        }

        return r.trimEnd()
    }
}
