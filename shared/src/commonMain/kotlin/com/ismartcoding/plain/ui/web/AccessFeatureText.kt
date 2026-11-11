package com.ismartcoding.plain.ui.web

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.features.web.AccessFeature
import com.ismartcoding.plain.i18n.*
import org.jetbrains.compose.resources.stringResource

@Composable
fun AccessFeature.getText(): String = stringResource(when (this) {
    AccessFeature.IMAGES -> Res.string.images
    AccessFeature.VIDEOS -> Res.string.videos
    AccessFeature.AUDIO -> Res.string.audios
    AccessFeature.DOCS -> Res.string.docs
    AccessFeature.FILES -> Res.string.feature_WRITE_EXTERNAL_STORAGE
    AccessFeature.CONTACTS -> Res.string.feature_WRITE_CONTACTS
    AccessFeature.SMS -> Res.string.feature_READ_SMS
    AccessFeature.CALLS -> Res.string.feature_WRITE_CALL_LOG
    AccessFeature.CALL_PHONE -> Res.string.feature_CALL_PHONE
    AccessFeature.PHONE_NUMBER -> Res.string.feature_READ_PHONE_NUMBERS
    AccessFeature.APPS -> Res.string.feature_QUERY_ALL_PACKAGES
    AccessFeature.NOTIFICATIONS -> Res.string.feature_NOTIFICATION_LISTENER
    AccessFeature.CLIPBOARD -> Res.string.clipboard_sync
    AccessFeature.ADB -> Res.string.adb_automation
})
