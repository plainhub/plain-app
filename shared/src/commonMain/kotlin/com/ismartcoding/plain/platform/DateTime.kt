package com.ismartcoding.plain.platform

import kotlin.time.Instant

internal object InstantFormats {
    const val FILE_NAME_DATE_FORMAT = "yyyyMMdd_HHmmss_SSS"
}

expect fun Instant.formatDateTime(): String

expect fun Instant.formatDate(): String

expect fun Instant.formatTime(): String

expect fun Instant.formatName(): String
