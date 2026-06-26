package com.ismartcoding.plain.helpers

object TimeAgoHelper {
    fun getString(ms: Long): String = RelativeTimeFormatter.format(ms)
}