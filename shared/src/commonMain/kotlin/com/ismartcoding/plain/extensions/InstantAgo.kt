package com.ismartcoding.plain.extensions

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.helpers.RelativeTimeFormatter
import kotlin.time.Instant

@Composable
fun Instant.timeAgo(): String {
    return RelativeTimeFormatter.format(toEpochMilliseconds())
}