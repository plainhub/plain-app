package com.ismartcoding.plain.platform.permissions

import com.ismartcoding.plain.features.web.AccessFeature
import com.ismartcoding.plain.i18n.*

actual val supportsDirectorySelection: Boolean = false

actual fun AccessFeature.systemPermissions(): Set<Permission> = when (this) {
    AccessFeature.FILES -> setOf(Permission.WRITE_EXTERNAL_STORAGE)
    AccessFeature.IMAGES -> setOf(Permission.WRITE_EXTERNAL_STORAGE)
    AccessFeature.VIDEOS -> setOf(Permission.WRITE_EXTERNAL_STORAGE)
    AccessFeature.AUDIO -> setOf(Permission.WRITE_EXTERNAL_STORAGE)
    AccessFeature.DOCS -> setOf(Permission.WRITE_EXTERNAL_STORAGE)
    AccessFeature.CONTACTS -> setOf(Permission.READ_CONTACTS, Permission.WRITE_CONTACTS)
    AccessFeature.SMS -> setOf(Permission.READ_SMS, Permission.SEND_SMS)
    AccessFeature.CALLS -> setOf(Permission.READ_CALL_LOG, Permission.WRITE_CALL_LOG)
    AccessFeature.CALL_PHONE -> setOf(Permission.CALL_PHONE)
    AccessFeature.PHONE_NUMBER -> setOf(Permission.READ_PHONE_STATE, Permission.READ_PHONE_NUMBERS)
    AccessFeature.APPS -> setOf(Permission.QUERY_ALL_PACKAGES)
    AccessFeature.NOTIFICATIONS -> setOf(Permission.NOTIFICATION_LISTENER)
    AccessFeature.CLIPBOARD -> setOf(Permission.CLIPBOARD)
    AccessFeature.ADB -> setOf(Permission.ADB)
}

internal actual suspend fun Set<AccessFeature>.requestSystemAccess() {
    val permissions = flatMap { it.systemPermissions() }.distinct().filter { !it.isGranted() }
    if (permissions.isNotEmpty()) com.ismartcoding.plain.lib.sendEvent(
        com.ismartcoding.plain.events.RequestPermissionsEvent(*permissions.toTypedArray(), showDeniedDialog = false),
    )
}

internal actual fun Set<AccessFeature>.accessIssues(): List<AccessIssue> {
    val missing = filterNot { it.isGranted() }.toSet()
    if (missing.isEmpty()) return emptyList()
    val files = missing.any { it in setOf(AccessFeature.IMAGES, AccessFeature.VIDEOS, AccessFeature.AUDIO, AccessFeature.DOCS, AccessFeature.FILES) }
    return listOf(AccessIssue(
        if (files) Res.string.access_files_help else Res.string.access_feature_help,
        missing, AccessAction.REQUEST, Res.string.access_allow,
    ))
}
