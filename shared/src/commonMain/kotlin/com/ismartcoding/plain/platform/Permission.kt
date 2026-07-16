package com.ismartcoding.plain.platform

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.lib.channel.sendEvent
import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.preferences.ApiPermissionsPreference

/**
 * Check whether a runtime permission is granted without prompting the user.
 *
 * @param perm Android: `android.Manifest.permission.*` (e.g. "android.permission.CAMERA").
 *             iOS: empty string (returns true on iOS, all permissions queried via OS APIs).
 */
expect fun isPermissionGranted(perm: String): Boolean

enum class Permission {
    WRITE_EXTERNAL_STORAGE,
    READ_SMS,
    SEND_SMS,
    READ_CONTACTS,
    WRITE_CONTACTS,
    READ_CALL_LOG,
    WRITE_CALL_LOG,
    CALL_PHONE,
    POST_NOTIFICATIONS,
    NEARBY_WIFI_DEVICES,
    ACCESS_FINE_LOCATION,
    CAMERA,
    SYSTEM_ALERT_WINDOW,
    RECORD_AUDIO,
    READ_MEDIA_IMAGES,
    READ_MEDIA_VIDEOS,
    READ_MEDIA_AUDIO,
    NOTIFICATION_LISTENER,
    READ_PHONE_STATE,
    READ_PHONE_NUMBERS,
    SCHEDULE_EXACT_ALARM,
    QUERY_ALL_PACKAGES,
    NONE
    ;

    @Composable
    fun getText(): String {
        return when (this) {
            NONE -> stringResource(Res.string.open_permission_settings)
            WRITE_EXTERNAL_STORAGE -> stringResource(Res.string.feature_WRITE_EXTERNAL_STORAGE)
            READ_SMS -> stringResource(Res.string.feature_READ_SMS)
            SEND_SMS -> stringResource(Res.string.feature_SEND_SMS)
            WRITE_CALL_LOG -> stringResource(Res.string.feature_WRITE_CALL_LOG)
            CALL_PHONE -> stringResource(Res.string.feature_CALL_PHONE)
            WRITE_CONTACTS -> stringResource(Res.string.feature_WRITE_CONTACTS)
            NOTIFICATION_LISTENER -> stringResource(Res.string.feature_NOTIFICATION_LISTENER)
            READ_PHONE_NUMBERS -> stringResource(Res.string.feature_READ_PHONE_NUMBERS)
            QUERY_ALL_PACKAGES -> stringResource(Res.string.feature_QUERY_ALL_PACKAGES)
            else -> ""
        }
    }

    fun toSysPermission(): String {
        return "android.permission.${this.name}"
    }

    @Composable
    fun getGrantAccessText(): String {
        return when {
            this == READ_SMS -> stringResource(Res.string.need_sms_permission)
            this == READ_CALL_LOG -> stringResource(Res.string.need_call_permission)
            this == READ_CONTACTS -> stringResource(Res.string.need_contact_permission)
            this == WRITE_EXTERNAL_STORAGE -> stringResource(Res.string.need_storage_permission)
            else -> ""
        }
    }
}

expect fun Permission.isGranted(): Boolean

/**
 * Ensure the POST_NOTIFICATIONS runtime permission is granted, prompting the
 * user if necessary. Returns true once the permission is granted (or already
 * was), false if the user denied it. No-op (always returns true) on platforms
 * without a notification permission concept (iOS).
 *
 * Equivalent to the Android-only `Permissions.ensureNotificationAsync(context)`
 * but uses the platform's app context internally so it can be called from
 * commonMain.
 */
expect suspend fun ensureNotificationPermissionAsync(): Boolean

/**
 * Check the POST_NOTIFICATIONS permission and run [onGranted] once it is granted
 * (immediately if already granted, after the user grants it via the dialog
 * otherwise). On iOS, [onGranted] is invoked immediately since iOS has no
 * equivalent runtime permission concept for notifications.
 *
 * Equivalent to the Android-only `Permissions.checkNotification(context, ...)`.
 *
 * @param stringResource The resource id of the message shown in the
 *     confirmation dialog when the permission is not yet granted.
 */
expect fun checkNotificationPermission(stringResource: StringResource, onGranted: () -> Unit)

fun Permission.grant(): Boolean {
    if (isGranted()) return true
    sendEvent(RequestPermissionsEvent(this))
    return false
}

suspend fun Permission.isEnabledAsync(): Boolean {
    return ApiPermissionsPreference.getAsync().contains(name)
}

suspend fun Permission.enabledAndIsGrantedAsync(): Boolean {
    return isGranted() && isEnabledAsync()
}

suspend fun Permission.checkEnabledAsync() {
    if (!isEnabledAsync()) {
        throw Exception("no_permission")
    }
}
