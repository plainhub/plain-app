package com.ismartcoding.plain.events

import com.ismartcoding.plain.platform.permissions.Permission
import com.ismartcoding.plain.lib.ChannelEvent

class RequestPermissionsEvent(vararg val permissions: Permission, val showDeniedDialog: Boolean = true) : ChannelEvent()

class PermissionsResultEvent(val map: Map<String, Boolean>) : ChannelEvent() {
    fun has(permission: Permission): Boolean {
        return map.containsKey(permission.toSysPermission())
    }
}
