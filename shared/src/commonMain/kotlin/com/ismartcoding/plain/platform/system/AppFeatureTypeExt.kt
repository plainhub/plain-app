package com.ismartcoding.plain.platform.system

import com.ismartcoding.plain.features.permissions.allGranted
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.platform.system.getDeviceCapabilities

private fun AppFeatureType.requiredCapability(): Capability? = when (this) {
        AppFeatureType.SOUND_METER -> Capability.SOUND_METER
        AppFeatureType.APPS -> Capability.PACKAGES
        AppFeatureType.NOTIFICATIONS -> Capability.NOTIFICATIONS
        AppFeatureType.IMAGE_EDITOR -> Capability.IMAGE_EDITOR
        AppFeatureType.CHECK_UPDATES -> Capability.APP_UPDATES
        else -> null
    }

fun AppFeatureType.has(): Boolean = requiredCapability()?.let { it in getDeviceCapabilities() } ?: true

internal fun AppFeatureType.has(capabilities: Collection<Capability>): Boolean = requiredCapability()?.let { it in capabilities } ?: true

fun AppFeatureType.hasPermission(): Boolean = getPermission()?.let { allGranted(it.permissions) } ?: true
