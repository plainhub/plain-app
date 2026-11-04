package com.ismartcoding.plain.platform.permissions

import com.ismartcoding.plain.events.RequestPermissionsEvent
import com.ismartcoding.plain.lib.sendEvent
import com.ismartcoding.plain.Prefs
import kotlinx.serialization.Serializable

@Serializable
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
    ADB,
    CLIPBOARD
    ;

    fun toSysPermission(): String {
        return "android.permission.${this.name}"
    }

}

expect fun Permission.isGranted(): Boolean

/**
 * True when the user denied this permission before but the runtime dialog can
 * still be shown (i.e., not a permanent "don't ask again" denial). Only
 * meaningful after an actual denial; false on platforms without the concept.
 */
expect fun Permission.shouldShowRationale(): Boolean

expect suspend fun ensureNotificationPermissionAsync(): Boolean

fun Permission.grant(): Boolean {
    if (isGranted()) return true
    sendEvent(RequestPermissionsEvent(this))
    return false
}

suspend fun Permission.isEnabledAsync(): Boolean =
    name in Prefs.apiPermissions.value

/** Enabled for the web client *and* granted by the OS. Both answers are local. */
suspend fun Permission.enabledAndIsGrantedAsync(): Boolean = isEnabledAsync() && isGranted()

suspend fun Permission.checkEnabledAsync() {
    if (!isEnabledAsync()) {
        throw Exception("no_permission")
    }
}


expect fun imageLibraryPermission(): FeaturePermission
