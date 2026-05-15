package com.ismartcoding.plain.helpers

/** Backward-compat shim — delegates to [RelativeTimeFormatter]. */
object TimeAgoHelper {
    fun getString(ms: Long): String = RelativeTimeFormatter.format(ms)
}
