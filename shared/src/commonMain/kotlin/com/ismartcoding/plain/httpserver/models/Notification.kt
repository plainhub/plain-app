package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.data.DNotification
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLField
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@GraphQLType
@Serializable
data class Notification(
    val id: ID,
    @GraphQLField(description = "Android FLAG_ONLY_ALERT_ONCE: re-posted updates must not sound/vibrate again.")
    val onlyOnce: Boolean,
    val isClearable: Boolean,
    val appId: ID,
    val appName: String,
    val postedAt: Instant,
    val silent: Boolean,
    val title: String,
    val body: String,
    val actions: List<String>,
    @GraphQLField(description = "Subset of `actions` that support inline reply; `replyNotification`'s `actionIndex` indexes this list, not `actions`.")
    val replyActions: List<String>
)

fun DNotification.toModel(): Notification {
    return Notification(ID(id), onlyOnce, isClearable, ID(appId), appName, time, silent, title, body, actions, replyActions)
}
