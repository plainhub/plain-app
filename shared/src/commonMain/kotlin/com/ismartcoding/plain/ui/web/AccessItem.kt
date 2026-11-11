package com.ismartcoding.plain.ui.web

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.features.web.AccessFeature
import com.ismartcoding.plain.i18n.*
import com.ismartcoding.plain.platform.permissions.AccessIssue
import com.ismartcoding.plain.platform.permissions.accessIssues
import org.jetbrains.compose.resources.stringResource

internal val managedFileAccessFeatures = setOf(
    AccessFeature.IMAGES, AccessFeature.VIDEOS, AccessFeature.AUDIO,
    AccessFeature.DOCS, AccessFeature.FILES,
)

internal data class AccessItem(
    val features: Set<AccessFeature>,
    internal val issues: List<AccessIssue> = features.accessIssues(),
) {
    fun isEnabled(enabled: Set<AccessFeature>): Boolean = enabled.containsAll(features)

    @Composable
    fun getText(): String {
        return if (features == managedFileAccessFeatures) stringResource(Res.string.files)
        else stringResource(when (features.single()) {
            AccessFeature.CONTACTS -> Res.string.access_contacts
            AccessFeature.SMS -> Res.string.access_sms
            AccessFeature.CALLS -> Res.string.access_call_logs
            AccessFeature.PHONE_NUMBER -> Res.string.access_phone_number
            AccessFeature.APPS -> Res.string.apps
            AccessFeature.NOTIFICATIONS -> Res.string.perm_wizard_notif_name
            else -> return features.single().getText()
        })
    }

    @Composable
    fun getDescription(): String = stringResource(when {
        features == managedFileAccessFeatures -> Res.string.access_files_description
        else -> when (features.single()) {
            AccessFeature.CONTACTS -> Res.string.access_contacts_description
            AccessFeature.SMS -> Res.string.access_sms_description
            AccessFeature.CALLS -> Res.string.access_call_logs_description
            AccessFeature.CALL_PHONE -> Res.string.access_call_phone_description
            AccessFeature.PHONE_NUMBER -> Res.string.access_phone_number_description
            AccessFeature.APPS -> Res.string.access_apps_description
            AccessFeature.NOTIFICATIONS -> Res.string.access_notifications_description
            AccessFeature.CLIPBOARD -> Res.string.access_clipboard_description
            else -> error("Feature has no access settings row: $features")
        }
    })
}
