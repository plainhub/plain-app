package com.ismartcoding.plain.features.permissions


import com.ismartcoding.plain.platform.permissions.isGranted
import com.ismartcoding.plain.platform.permissions.Permission

import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.platform.system.getDeviceCapabilities
import com.ismartcoding.plain.platform.system.has
import com.ismartcoding.plain.ui.resources.Res as UiRes
import com.ismartcoding.plain.ui.resources.call_log as ui_drawable_call_log
import com.ismartcoding.plain.ui.resources.contact_round as ui_drawable_contact_round
import com.ismartcoding.plain.ui.resources.file_digit as ui_drawable_file_digit
import com.ismartcoding.plain.ui.resources.folder as ui_drawable_folder
import com.ismartcoding.plain.ui.resources.message_square_text as ui_drawable_message_square_text
import com.ismartcoding.plain.ui.resources.package2 as ui_drawable_package2
import com.ismartcoding.plain.ui.resources.phone_call as ui_drawable_phone_call

import com.ismartcoding.plain.ui.resources.image as access_image
import com.ismartcoding.plain.ui.resources.video as access_video
import com.ismartcoding.plain.ui.resources.music as access_music
import com.ismartcoding.plain.ui.resources.file_text as access_document
import com.ismartcoding.plain.ui.resources.bell as access_bell
import com.ismartcoding.plain.ui.resources.content_paste as access_clipboard
import com.ismartcoding.plain.ui.resources.code as access_code

fun allGranted(permissions: Set<Permission>): Boolean {
    return permissions.all { it.isGranted() }
}

internal fun getWebList(): List<com.ismartcoding.plain.ui.web.AccessItem> = buildList {
    val capabilities = getDeviceCapabilities()
    add(com.ismartcoding.plain.ui.web.AccessItem(UiRes.drawable.ui_drawable_folder, com.ismartcoding.plain.ui.web.managedFileAccessFeatures))
    for (feature in com.ismartcoding.plain.features.web.AccessFeature.entries.filterNot { it in com.ismartcoding.plain.ui.web.managedFileAccessFeatures || it == com.ismartcoding.plain.features.web.AccessFeature.NOTIFICATIONS || it == com.ismartcoding.plain.features.web.AccessFeature.CLIPBOARD }) {
        val supported = when (feature) {
            com.ismartcoding.plain.features.web.AccessFeature.CONTACTS,
            com.ismartcoding.plain.features.web.AccessFeature.SMS,
            com.ismartcoding.plain.features.web.AccessFeature.CALLS,
            com.ismartcoding.plain.features.web.AccessFeature.CALL_PHONE -> Capability.TELEPHONY in capabilities
            com.ismartcoding.plain.features.web.AccessFeature.PHONE_NUMBER -> Capability.TELEPHONY in capabilities
            com.ismartcoding.plain.features.web.AccessFeature.APPS -> AppFeatureType.APPS.has()
            com.ismartcoding.plain.features.web.AccessFeature.NOTIFICATIONS -> AppFeatureType.NOTIFICATIONS.has()
            com.ismartcoding.plain.features.web.AccessFeature.ADB -> Capability.ADB in capabilities
            else -> true
        }
        val icon = when (feature) {
            com.ismartcoding.plain.features.web.AccessFeature.IMAGES -> UiRes.drawable.access_image
            com.ismartcoding.plain.features.web.AccessFeature.VIDEOS -> UiRes.drawable.access_video
            com.ismartcoding.plain.features.web.AccessFeature.AUDIO -> UiRes.drawable.access_music
            com.ismartcoding.plain.features.web.AccessFeature.DOCS -> UiRes.drawable.access_document
            com.ismartcoding.plain.features.web.AccessFeature.CONTACTS -> UiRes.drawable.ui_drawable_contact_round
            com.ismartcoding.plain.features.web.AccessFeature.SMS -> UiRes.drawable.ui_drawable_message_square_text
            com.ismartcoding.plain.features.web.AccessFeature.CALLS -> UiRes.drawable.ui_drawable_call_log
            com.ismartcoding.plain.features.web.AccessFeature.CALL_PHONE -> UiRes.drawable.ui_drawable_phone_call
            com.ismartcoding.plain.features.web.AccessFeature.PHONE_NUMBER -> UiRes.drawable.ui_drawable_file_digit
            com.ismartcoding.plain.features.web.AccessFeature.APPS -> UiRes.drawable.ui_drawable_package2
            com.ismartcoding.plain.features.web.AccessFeature.NOTIFICATIONS -> UiRes.drawable.access_bell
            com.ismartcoding.plain.features.web.AccessFeature.CLIPBOARD -> UiRes.drawable.access_clipboard
            com.ismartcoding.plain.features.web.AccessFeature.ADB -> UiRes.drawable.access_code
            com.ismartcoding.plain.features.web.AccessFeature.FILES -> UiRes.drawable.ui_drawable_folder
        }
        if (supported) add(com.ismartcoding.plain.ui.web.AccessItem(icon, setOf(feature)))
    }
}
