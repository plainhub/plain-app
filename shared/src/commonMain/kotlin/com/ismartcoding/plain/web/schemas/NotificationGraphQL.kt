package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLMutation
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLQuery
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.events.HCancelNotificationsEvent
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.platform.filterNotificationsAsync
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.Notification
import com.ismartcoding.plain.web.models.toModel

@GraphQLQuery
suspend fun notifications(): List<Notification> {
    Permission.NOTIFICATION_LISTENER.checkEnabledAsync()
    return filterNotificationsAsync().sortedByDescending { it.time }.map { it.toModel() }
}

@GraphQLMutation
suspend fun cancelNotifications(ids: List<ID>): Boolean {
    sendEvent(HCancelNotificationsEvent(ids.map { it.value }.toSet()))
    return true
}

@GraphQLMutation
suspend fun replyNotification(id: ID, actionIndex: Int, text: String): Boolean {
    val ok = com.ismartcoding.plain.platform.replyNotification(id.value, actionIndex, text)
    if (!ok) {
        throw GraphQLError("action_not_found")
    }
    return true
}

fun SchemaBuilder.addNotificationSchema() {
}
