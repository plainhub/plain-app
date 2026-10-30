package com.ismartcoding.plain.ui.web

import com.ismartcoding.plain.features.web.WebSettingsFeature
import com.ismartcoding.plain.platform.permissions.Permission

internal fun Permission.accessFeatureOf(): WebSettingsFeature? = when (this) {
    Permission.WRITE_EXTERNAL_STORAGE -> WebSettingsFeature.FILES
    Permission.WRITE_CONTACTS -> WebSettingsFeature.CONTACTS
    Permission.READ_SMS -> WebSettingsFeature.SMS
    Permission.WRITE_CALL_LOG -> WebSettingsFeature.CALL_LOGS
    Permission.CALL_PHONE -> WebSettingsFeature.CALL_PHONE
    Permission.READ_PHONE_NUMBERS -> WebSettingsFeature.PHONE_NUMBER
    Permission.QUERY_ALL_PACKAGES -> WebSettingsFeature.APPS
    Permission.NOTIFICATION_LISTENER -> WebSettingsFeature.NOTIFICATIONS
    Permission.CLIPBOARD -> WebSettingsFeature.CLIPBOARD
    else -> null
}
