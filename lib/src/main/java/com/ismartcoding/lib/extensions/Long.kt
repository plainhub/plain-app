package com.ismartcoding.lib.extensions

import java.text.StringCharacterIterator
import java.util.concurrent.TimeUnit

fun Long.formatBitrate(): String {
    if (this in 0..999) {
        return "$this bit/s"
    }

    var newBytes = this
    val ci = StringCharacterIterator("kMGTPE")
    while (newBytes <= -999950 || newBytes >= 999950) {
        newBytes /= 1000
        ci.next()
    }

    return String.format("%.1f %cbit/s", newBytes / 1000.0, ci.current())
}

fun Long.formatBytes(): String {
    if (this in 0..999) {
        return "$this B"
    }

    var newBytes = this
    val ci = StringCharacterIterator("kMGTPE")
    while (newBytes <= -999950 || newBytes >= 999950) {
        newBytes /= 1000
        ci.next()
    }

    return String.format("%.1f %cB", newBytes / 1000.0, ci.current())
}

fun Long.formatDuration(
    alwaysShowHour: Boolean = false,
): String {
    val totalSeconds = this
    val seconds = totalSeconds % 60
    val minutes = totalSeconds / 60 % 60
    val hours = totalSeconds / 3600
    return if (hours > 0 || alwaysShowHour) {
        String.format("%02d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format("%02d:%02d", minutes, seconds)
    }
}

fun Long.formatMinSec(): String {
    return if (this == 0L) {
        "00:00"
    } else {
        String.format(
            "%02d:%02d",
            TimeUnit.MILLISECONDS.toMinutes(this),
            TimeUnit.MILLISECONDS.toSeconds(this) -
                    TimeUnit.MINUTES.toSeconds(
                        TimeUnit.MILLISECONDS.toMinutes(this)
                    )
        )
    }
}