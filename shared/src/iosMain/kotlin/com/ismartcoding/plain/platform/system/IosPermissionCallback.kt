package com.ismartcoding.plain.platform.system

import com.ismartcoding.plain.platform.permissions.Permission

import com.ismartcoding.plain.events.PermissionsResultEvent
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.lib.sendEvent

object IosPermissionCallback {

    fun onPermissionResult(permissionName: String, granted: Boolean) {
        LogCat.d("IosPermissionCallback: $permissionName granted=$granted")
        val permission = runCatching { Permission.valueOf(permissionName) }.getOrNull()
        val key = permission?.toSysPermission() ?: permissionName
        sendEvent(PermissionsResultEvent(mapOf(key to granted)))
    }
}
