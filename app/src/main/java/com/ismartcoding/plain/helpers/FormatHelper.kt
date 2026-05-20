package com.ismartcoding.plain.helpers

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.i18n.*

import org.jetbrains.compose.resources.PluralStringResource
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.StringCharacterIterator
import java.util.Currency
import java.util.Locale

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

    fun formatMoney(
        value: Double,
        currencyCode: String,
    ): String {
        val format = NumberFormat.getCurrencyInstance(Locale.getDefault())
        if (format is DecimalFormat) {
            val dfs = DecimalFormat().decimalFormatSymbols
            dfs.currency = Currency.getInstance(currencyCode)
            format.decimalFormatSymbols = dfs
            format.minimumFractionDigits = 2
            format.maximumFractionDigits = 2
            format.isGroupingUsed = true
            format.roundingMode = RoundingMode.HALF_UP
        }

        return format.format(value)
    }

    fun formatDouble(
        value: Double,
        digits: Int = 2,
        isGroupingUsed: Boolean = true,
    ): String {
        val format = DecimalFormat()
        format.minimumFractionDigits = digits
        format.maximumFractionDigits = digits
        format.isGroupingUsed = isGroupingUsed
        format.roundingMode = RoundingMode.HALF_UP
        return format.format(value)
    }

    fun formatFloat(
        value: Float,
        digits: Int = 2,
        isGroupingUsed: Boolean = true,
    ): String {
        val format = DecimalFormat()
        format.minimumFractionDigits = digits
        format.maximumFractionDigits = digits
        format.isGroupingUsed = isGroupingUsed
        format.roundingMode = RoundingMode.HALF_UP
        return format.format(value)
    }
}
