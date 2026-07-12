package com.ismartcoding.plain.lib.extensions

private const val SI_PREFIXES = "kMGTPE"

fun Long.formatBitrate(): String {
    if (this in 0..999) {
        return "$this bit/s"
    }

    var newBytes = this
    var prefixIndex = 0
    while (newBytes <= -999950 || newBytes >= 999950) {
        newBytes /= 1000
        prefixIndex++
    }

    val prefix = SI_PREFIXES.getOrElse(prefixIndex) { '?' }
    return formatOneDecimal(newBytes / 1000.0) + " ${prefix}bit/s"
}

fun Long.formatBytes(): String {
    if (this in 0..999) {
        return "$this B"
    }

    var newBytes = this
    var prefixIndex = 0
    while (newBytes <= -999950 || newBytes >= 999950) {
        newBytes /= 1000
        prefixIndex++
    }

    val prefix = SI_PREFIXES.getOrElse(prefixIndex) { '?' }
    return formatOneDecimal(newBytes / 1000.0) + " ${prefix}B"
}

fun Long.formatDuration(
    alwaysShowHour: Boolean = false,
): String {
    val totalSeconds = this
    val seconds = totalSeconds % 60
    val minutes = totalSeconds / 60 % 60
    val hours = totalSeconds / 3600
    return if (hours > 0 || alwaysShowHour) {
        "${hours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    } else {
        "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    }
}

fun Long.formatMinSec(): String {
    if (this == 0L) {
        return "00:00"
    }
    val totalSeconds = this / 1000
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
}

private fun formatOneDecimal(value: Double): String {
    val rounded = kotlin.math.round(value * 10.0).toInt()
    val integerPart = rounded / 10
    val fractionalPart = (rounded % 10).let { if (it < 0) -it else it }
    return "$integerPart.$fractionalPart"
}
