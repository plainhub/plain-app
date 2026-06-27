package com.ismartcoding.plain.extensions

import com.ismartcoding.plain.helpers.TimeAgoHelper
import kotlin.time.Instant

fun Instant.timeAgo(): String {
    return TimeAgoHelper.getString(toEpochMilliseconds())
}