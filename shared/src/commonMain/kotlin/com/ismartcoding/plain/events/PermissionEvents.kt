package com.ismartcoding.plain.events

import com.ismartcoding.plain.platform.Permission
import com.ismartcoding.plain.lib.channel.ChannelEvent

class RequestPermissionsEvent(vararg val permissions: Permission) : ChannelEvent()

class PermissionsResultEvent(val map: Map<String, Boolean>) : ChannelEvent() {
    fun has(permission: Permission): Boolean {
        return map.containsKey(permission.toSysPermission())
    }
}
