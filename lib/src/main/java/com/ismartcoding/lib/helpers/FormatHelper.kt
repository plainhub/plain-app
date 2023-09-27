package com.ismartcoding.lib.helpers

import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.NumberFormat
import java.text.StringCharacterIterator
import java.util.*

object FormatHelper {
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

    fun formatDuration(
        totalSeconds: Long,
        alwaysShowHour: Boolean = false,
    ): String {
        val seconds = totalSeconds % 60
        val minutes = totalSeconds / 60 % 60
        val hours = totalSeconds / 3600
        return if (hours > 0 || alwaysShowHour) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    fun formatBytes(bytes: Long): String {
        if (bytes in 0..999) {
            return "$bytes B"
        }

        var newBytes = bytes
        val ci = StringCharacterIterator("kMGTPE")
        while (newBytes <= -999950 || newBytes >= 999950) {
            newBytes /= 1000
            ci.next()
        }

        return String.format("%.1f %cB", newBytes / 1000.0, ci.current())
    }
}
