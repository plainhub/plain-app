package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DNotification
import kotlinx.datetime.Instant

data class Notification(
    val id: String,
    val onlyOnce: Boolean,
    val isClearable: Boolean,
    val appName: String,
    val time: Instant,
    val silent: Boolean,
    val title: String,
    val text: String,
    val actions: List<String>
)

fun DNotification.toModel(): Notification {
    return Notification(id, onlyOnce, isClearable, appName, time, silent, title, text, actions)
}
