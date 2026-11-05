package com.ismartcoding.plain.ui.web

import com.ismartcoding.plain.features.web.AccessFeature
import com.ismartcoding.plain.platform.permissions.Permission

internal fun Permission.accessFeatureOf(): AccessFeature = when (this) {
    Permission.WRITE_EXTERNAL_STORAGE -> AccessFeature.FILES
    Permission.WRITE_CONTACTS -> AccessFeature.CONTACTS
    Permission.READ_CONTACTS -> AccessFeature.CONTACTS
    Permission.READ_SMS -> AccessFeature.SMS
    Permission.SEND_SMS -> AccessFeature.SMS
    Permission.WRITE_CALL_LOG -> AccessFeature.CALL_LOGS
    Permission.READ_CALL_LOG -> AccessFeature.CALL_LOGS
    Permission.CALL_PHONE -> AccessFeature.CALL_PHONE
    Permission.READ_PHONE_NUMBERS -> AccessFeature.PHONE_NUMBER
    Permission.QUERY_ALL_PACKAGES -> AccessFeature.APPS
    Permission.NOTIFICATION_LISTENER -> AccessFeature.NOTIFICATIONS
    Permission.CLIPBOARD -> AccessFeature.CLIPBOARD
    Permission.ADB -> AccessFeature.ADB
    Permission.READ_MEDIA_IMAGES -> AccessFeature.IMAGES
    Permission.READ_MEDIA_VIDEOS -> AccessFeature.VIDEOS
    Permission.READ_MEDIA_AUDIO -> AccessFeature.AUDIO
    else -> error("Permission has no remote access feature: $this")
}
