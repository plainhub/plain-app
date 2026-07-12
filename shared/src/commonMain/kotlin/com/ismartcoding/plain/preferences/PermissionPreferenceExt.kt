package com.ismartcoding.plain.preferences

import com.ismartcoding.plain.platform.Permission

suspend fun ApiPermissionsPreference.putAsync(permission: Permission, enable: Boolean) {
    val permissions = getAsync().toMutableSet()
    if (enable) permissions.add(permission.name) else permissions.remove(permission.name)
    putAsync(permissions)
}
