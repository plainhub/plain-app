package com.ismartcoding.plain.features.permissions

import com.ismartcoding.plain.features.web.AccessFeature
import com.ismartcoding.plain.platform.permissions.Permission
import com.ismartcoding.plain.platform.permissions.isGranted
import com.ismartcoding.plain.platform.system.AppFeatureType
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.platform.system.getDeviceCapabilities
import com.ismartcoding.plain.platform.system.has
import com.ismartcoding.plain.ui.web.AccessItem
import com.ismartcoding.plain.ui.web.managedFileAccessFeatures

fun allGranted(permissions: Set<Permission>): Boolean = permissions.all { it.isGranted() }

internal fun getWebList(): List<AccessItem> = buildList {
    val capabilities = getDeviceCapabilities()
    add(AccessItem(managedFileAccessFeatures))
    for (feature in AccessFeature.entries.filterNot { it in managedFileAccessFeatures || it == AccessFeature.ADB }) {
        val supported = when (feature) {
            AccessFeature.CONTACTS, AccessFeature.SMS, AccessFeature.CALLS,
            AccessFeature.CALL_PHONE, AccessFeature.PHONE_NUMBER -> Capability.TELEPHONY in capabilities
            AccessFeature.APPS -> AppFeatureType.APPS.has()
            AccessFeature.NOTIFICATIONS -> AppFeatureType.NOTIFICATIONS.has()
            else -> true
        }
        if (supported) add(AccessItem(setOf(feature)))
    }
}
