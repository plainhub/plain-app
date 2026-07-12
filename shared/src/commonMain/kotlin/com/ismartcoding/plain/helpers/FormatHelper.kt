package com.ismartcoding.plain.helpers

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*
import kotlin.math.pow
import kotlin.math.round
import kotlin.math.roundToLong
import org.jetbrains.compose.resources.PluralStringResource

object FormatHelper {
    @Composable
    fun formatSeconds(n: Int, plural: @Composable (PluralStringResource, Int, Int) -> String): String {
        val seconds = n % 60
        val minutes = n / 60 % 60
        val hours = n / 3600
        var r = ""
        if (hours > 0) {
            r += plural(Res.plurals.n_hours, hours, hours)
        }

        if (minutes > 0) {
            r += plural(Res.plurals.n_minutes, minutes, minutes)
        }

        if (seconds > 0) {
            r += plural(Res.plurals.n_seconds, seconds, seconds)
        }

        return r.trimEnd()
    }

    fun formatDouble(value: Double, digits: Int = 2, isGroupingUsed: Boolean = true): String {
        if (digits <= 0) {
            return round(value).roundToLong().toString()
        }
        val factor = 10.0.pow(digits)
        val rounded = round(value * factor) / factor
        val intPart = rounded.toLong()
        val fracPart = ((rounded - intPart) * factor).roundToLong()
        val fracStr = fracPart.toString().padStart(digits, '0').take(digits)
        return "$intPart.$fracStr"
    }

    fun formatFloat(value: Float, digits: Int = 2, isGroupingUsed: Boolean = true): String =
        formatDouble(value.toDouble(), digits, isGroupingUsed)
}
