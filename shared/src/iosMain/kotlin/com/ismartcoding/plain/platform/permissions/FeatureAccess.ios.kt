package com.ismartcoding.plain.platform.permissions

import com.ismartcoding.plain.features.web.AccessFeature
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.files.grantedStorageDirectories
import com.ismartcoding.plain.platform.system.IosPlatformRegistry
import platform.Photos.*
import kotlinx.coroutines.flow.onSubscription
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first

actual fun AccessFeature.systemPermissions(): Set<Permission> = when (this) {
    AccessFeature.FILES -> setOf(Permission.WRITE_EXTERNAL_STORAGE)
    AccessFeature.IMAGES, AccessFeature.VIDEOS -> if (grantedStorageDirectories().isEmpty()) setOf(Permission.READ_MEDIA_IMAGES) else emptySet()
    AccessFeature.AUDIO -> setOf(Permission.WRITE_EXTERNAL_STORAGE)
    AccessFeature.DOCS -> setOf(Permission.WRITE_EXTERNAL_STORAGE)
    AccessFeature.CONTACTS -> setOf(Permission.READ_CONTACTS, Permission.WRITE_CONTACTS)
    AccessFeature.SMS -> setOf(Permission.READ_SMS, Permission.SEND_SMS)
    AccessFeature.CALL_LOGS -> setOf(Permission.READ_CALL_LOG, Permission.WRITE_CALL_LOG)
    AccessFeature.CALL_PHONE -> setOf(Permission.CALL_PHONE)
    AccessFeature.PHONE_NUMBER -> setOf(Permission.READ_PHONE_STATE, Permission.READ_PHONE_NUMBERS)
    AccessFeature.APPS -> setOf(Permission.QUERY_ALL_PACKAGES)
    AccessFeature.NOTIFICATIONS -> setOf(Permission.NOTIFICATION_LISTENER)
    AccessFeature.CLIPBOARD -> setOf(Permission.CLIPBOARD)
    AccessFeature.ADB -> setOf(Permission.ADB)
}

internal actual suspend fun Set<AccessFeature>.requestSystemAccess() {
    val permissions = flatMap {
        if (it == AccessFeature.IMAGES || it == AccessFeature.VIDEOS) setOf(Permission.READ_MEDIA_IMAGES) else it.systemPermissions()
    }.distinct().filter { !it.isGranted() }
    for (permission in permissions) {
        com.ismartcoding.plain.lib.Channel.sharedFlow
            .onSubscription { com.ismartcoding.plain.lib.sendEvent(com.ismartcoding.plain.events.RequestPermissionsEvent(permission, showDeniedDialog = false)) }
            .filterIsInstance<com.ismartcoding.plain.events.PermissionsResultEvent>()
            .first { it.has(permission) }
    }
    if (any { it == AccessFeature.FILES || it == AccessFeature.AUDIO || it == AccessFeature.DOCS }) {
        com.ismartcoding.plain.lib.sendEvent(com.ismartcoding.plain.events.PickFileEvent(
            com.ismartcoding.plain.features.files.PickFileTag.DIRECTORY_ACCESS,
            com.ismartcoding.plain.features.files.PickFileType.FOLDER,
            multiple = false,
        ))
    }
}

@OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
internal actual fun Set<AccessFeature>.accessIssues(): List<AccessIssue> {
    val features = this
    return buildList {
        val photos = features.filter { it == AccessFeature.IMAGES || it == AccessFeature.VIDEOS }.toSet()
        if (photos.isNotEmpty()) {
            when (PHPhotoLibrary.authorizationStatusForAccessLevel(PHAccessLevelReadWrite)) {
                PHAuthorizationStatusNotDetermined -> add(AccessIssue(Res.string.access_photos_request_help, photos, AccessAction.REQUEST, Res.string.access_allow))
                PHAuthorizationStatusDenied -> add(AccessIssue(Res.string.access_photos_settings_help, photos, AccessAction.SETTINGS, Res.string.access_go_to_settings))
                PHAuthorizationStatusRestricted -> add(AccessIssue(Res.string.access_photos_restricted_help, photos))
                PHAuthorizationStatusLimited -> add(AccessIssue(Res.string.access_photos_limited_help, photos, AccessAction.SETTINGS, Res.string.access_go_to_settings, isError = false))
            }
        }
        if (features.any { it == AccessFeature.FILES || it == AccessFeature.AUDIO || it == AccessFeature.DOCS }) {
            val picker = checkNotNull(IosPlatformRegistry.filePicker())
            if (picker.hasUnavailableDirectories()) {
                add(AccessIssue(Res.string.access_folder_unavailable_help, setOf(AccessFeature.FILES), AccessAction.FOLDER, Res.string.access_choose_folder))
            } else if (picker.grantedDirectories().isEmpty()) {
                add(AccessIssue(Res.string.access_add_folder_help, setOf(AccessFeature.FILES), AccessAction.FOLDER, Res.string.add_folder, isError = false))
            }
        }
        val missing = features.filter { it !in photos && it != AccessFeature.FILES && it != AccessFeature.AUDIO && it != AccessFeature.DOCS && !it.isGranted() }.toSet()
        if (missing.isNotEmpty()) add(AccessIssue(Res.string.access_feature_help, missing, AccessAction.REQUEST, Res.string.access_allow))
    }
}
