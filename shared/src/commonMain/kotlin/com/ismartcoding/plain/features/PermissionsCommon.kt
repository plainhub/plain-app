package com.ismartcoding.plain.features
import com.ismartcoding.plain.platform.isGranted
import com.ismartcoding.plain.platform.Permission

import com.ismartcoding.plain.enums.AppFeatureType
import com.ismartcoding.plain.enums.has
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.preferences.ApiPermissionsPreference

suspend fun checkEnabledAsync(permissions: Set<Permission>) {
    val apiPermissions = ApiPermissionsPreference.getAsync().toMutableSet()
    if (apiPermissions.contains(Permission.WRITE_CONTACTS.toString())) {
        apiPermissions.add(Permission.READ_CONTACTS.toString())
    }
    if (apiPermissions.contains(Permission.WRITE_CALL_LOG.toString())) {
        apiPermissions.add(Permission.READ_CALL_LOG.toString())
    }
    for (item in permissions.map { it.toString() }) {
        if (!apiPermissions.contains(item)) {
            throw Exception("no_permission")
        }
    }
}

fun allGranted(permissions: Set<Permission>): Boolean {
    return permissions.all { it.isGranted() }
}

fun getWebList(): List<PermissionItem> {
    val list = mutableListOf<PermissionItem>()
    list.add(PermissionItem.create(Res.drawable.folder, Permission.WRITE_EXTERNAL_STORAGE))
    list.add(PermissionItem.create(Res.drawable.contact_round, Permission.WRITE_CONTACTS, setOf(Permission.READ_CONTACTS, Permission.WRITE_CONTACTS)))
    if (AppFeatureType.SMS.has()) {
        list.add(PermissionItem.create(Res.drawable.message_square_text, Permission.READ_SMS, setOf(Permission.READ_SMS)))
        list.add(PermissionItem.create(Res.drawable.message_square_text, Permission.SEND_SMS, setOf(Permission.SEND_SMS)))
    }
    if (AppFeatureType.CALLS.has()) {
        list.add(PermissionItem.create(Res.drawable.call_log, Permission.WRITE_CALL_LOG, setOf(Permission.READ_CALL_LOG, Permission.WRITE_CALL_LOG)))
    }
    list.add(PermissionItem.create(Res.drawable.phone_call, Permission.CALL_PHONE))
    list.add(PermissionItem.create(Res.drawable.file_digit, Permission.READ_PHONE_NUMBERS, setOf(Permission.READ_PHONE_STATE, Permission.READ_PHONE_NUMBERS)))
    if (AppFeatureType.APPS.has()) {
        list.add(PermissionItem.create(Res.drawable.package2, Permission.QUERY_ALL_PACKAGES))
    }
    return list
}

/**
 * The authoritative web-facing permission snapshot — the same list the
 * `app.permissions` GraphQL field returns. PERMISSIONS_UPDATED websocket
 * events carry this shape: a state snapshot, not a change set — clients
 * converge to it and derive enable/disable by diffing with their previous
 * snapshot.
 */
suspend fun getGrantedWebPermissionsAsync(): List<Permission> {
    val apiPermissions = ApiPermissionsPreference.getAsync()
    val granted = Permission.entries.filter { apiPermissions.contains(it.name) && it.isGranted() }.toMutableList()
    if (Permission.RECORD_AUDIO.isGranted() && !granted.contains(Permission.RECORD_AUDIO)) {
        granted.add(Permission.RECORD_AUDIO)
    }
    if (Permission.ADB.isGranted() && !granted.contains(Permission.ADB)) {
        granted.add(Permission.ADB)
    }
    return granted
}
