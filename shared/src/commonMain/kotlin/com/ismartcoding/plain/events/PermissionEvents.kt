package com.ismartcoding.plain.events

import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.lib.ChannelEvent

class RequestPermissionsEvent(vararg val permissions: Permission) : ChannelEvent()

// Asks for POST_NOTIFICATIONS through the runtime permission dialog first,
// unlike RequestPermissionsEvent(POST_NOTIFICATIONS) whose heuristic jumps to
// the system settings page once the permission was ever denied. Only falls
// back to the settings page when the dialog cannot be shown (pre-T devices).
class RequestNotificationPermissionEvent : ChannelEvent()

// Opens the system app-notification-settings screen directly. Unlike
// RequestPermissionsEvent(POST_NOTIFICATIONS), this never falls back to the
// runtime dialog, which Android silently denies once "don't ask again" is set.
class OpenNotificationSettingsEvent : ChannelEvent()

class PermissionsResultEvent(val map: Map<String, Boolean>) : ChannelEvent() {
    fun has(permission: Permission): Boolean {
        return map.containsKey(permission.toSysPermission())
    }
}
