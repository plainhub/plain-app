package com.ismartcoding.plain.web.schemas

import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.events.HCancelNotificationsEvent
import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.platform.checkEnabledAsync
import com.ismartcoding.plain.platform.filterNotificationsAsync
import com.ismartcoding.plain.platform.replyNotification
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addNotificationSchema() {
    query("notifications") {
        resolver { ->
            Permission.NOTIFICATION_LISTENER.checkEnabledAsync()
            filterNotificationsAsync().sortedByDescending { it.time }.map { it.toModel() }
        }
    }
    mutation("cancelNotifications") {
        resolver("ids") { ids: List<ID> ->
            sendEvent(HCancelNotificationsEvent(ids.map { it.value }.toSet()))
            true
        }
    }
    mutation("replyNotification") {
        resolver("id", "actionIndex", "text") { id: ID, actionIndex: Int, text: String ->
            val ok = replyNotification(id.value, actionIndex, text)
            if (!ok) {
                throw GraphQLError("action_not_found")
            }
            true
        }
    }
}
