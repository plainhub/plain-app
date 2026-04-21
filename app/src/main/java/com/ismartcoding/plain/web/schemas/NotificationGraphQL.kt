package com.ismartcoding.plain.web.schemas

import android.app.RemoteInput
import android.content.Intent
import android.os.Bundle
import com.ismartcoding.lib.kgraphql.GraphQLError
import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder
import com.ismartcoding.lib.channel.sendEvent
import com.ismartcoding.plain.MainApp
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.events.CancelNotificationsEvent
import com.ismartcoding.plain.features.Permission
import com.ismartcoding.plain.helpers.NotificationsHelper
import com.ismartcoding.plain.web.models.ID
import com.ismartcoding.plain.web.models.toModel

fun SchemaBuilder.addNotificationSchema() {
    query("notifications") {
        resolver { ->
            val context = MainApp.instance
            Permission.NOTIFICATION_LISTENER.checkAsync(context)
            NotificationsHelper.filterNotificationsAsync(context).sortedByDescending { it.time }.map { it.toModel() }
        }
    }
    mutation("cancelNotifications") {
        resolver { ids: List<ID> ->
            sendEvent(CancelNotificationsEvent(ids.map { it.value }.toSet()))
            true
        }
    }
    mutation("replyNotification") {
        resolver { id: ID, actionIndex: Int, text: String ->
            val actions = TempData.notificationActions[id.value]
                ?: throw GraphQLError("notification_not_found")
            // Only consider reply-capable actions (those with remoteInputs)
            val replyActions = actions.filter { it.remoteInputs != null && it.remoteInputs.isNotEmpty() }
            val action = replyActions.getOrNull(actionIndex)
                ?: throw GraphQLError("action_not_found")
            val remoteInputs = action.remoteInputs!!
            val remoteInput = remoteInputs.first()
            val intent = Intent()
            val bundle = Bundle()
            bundle.putCharSequence(remoteInput.resultKey, text)
            RemoteInput.addResultsToIntent(remoteInputs, intent, bundle) // uses android.app.RemoteInput
            action.actionIntent.send(MainApp.instance, 0, intent)
            true
        }
    }
}
