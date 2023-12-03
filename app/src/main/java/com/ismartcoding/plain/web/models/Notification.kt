package com.ismartcoding.plain.web.models

import com.ismartcoding.plain.data.DNotification
import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Notification(
    val id: ID,
    val onlyOnce: Boolean,
    val isClearable: Boolean,
    val appId: String,
    val appName: String,
    val time: Instant,
    val silent: Boolean,
    val title: String,
    val body: String,
    val actions: List<String>
)

fun DNotification.toModel(): Notification {
    return Notification(ID(id), onlyOnce, isClearable, appId, appName, time, silent, title, body, actions)
}
