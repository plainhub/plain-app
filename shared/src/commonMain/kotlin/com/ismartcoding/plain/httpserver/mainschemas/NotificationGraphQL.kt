package com.ismartcoding.plain.httpserver.mainschemas

import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.events.HCancelNotificationsEvent
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.helpers.QueryHelper
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.platform.filterNotificationsAsync
import com.ismartcoding.plain.httpserver.models.ActionResult
import com.ismartcoding.plain.httpserver.models.ID
import com.ismartcoding.plain.httpserver.models.Notification
import com.ismartcoding.plain.httpserver.models.toModel
import com.ismartcoding.plain.platform.replyNotification

@GraphQLQuery
suspend fun notifications(offset: Int, limit: Int, query: String): List<Notification> {
    Permission.NOTIFICATION_LISTENER.checkEnabledAsync()
    val q = QueryHelper.textOf(query).trim().lowercase()
    return filterNotificationsAsync()
        .filter { q.isEmpty() || it.appName.lowercase().contains(q) || it.title.lowercase().contains(q) || it.body.lowercase().contains(q) }
        .sortedByDescending { it.time }
        .drop(offset.coerceAtLeast(0))
        .take(limit.coerceAtLeast(0))
        .map { it.toModel() }
}

@GraphQLMutation
suspend fun deleteNotifications(ids: List<ID>): ActionResult {
    Permission.NOTIFICATION_LISTENER.checkEnabledAsync()
    sendEvent(HCancelNotificationsEvent(ids.map { it.value }.toSet()))
    return ActionResult(ids.size)
}

@GraphQLMutation(description = "Reply to a notification. actionIndex indexes Notification.replyActions (the reply-capable subset only), NOT the plain actions list.")
suspend fun replyNotification(id: ID, actionIndex: Int, text: String): Boolean {
    val ok = replyNotification(id.value, actionIndex, text)
    if (!ok) {
        throw GraphQLError("action_not_found")
    }
    return true
}

fun SchemaBuilder.addNotificationSchema() {
}
