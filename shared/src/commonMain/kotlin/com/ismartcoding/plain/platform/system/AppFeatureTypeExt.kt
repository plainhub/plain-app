package com.ismartcoding.plain.platform.system

import com.ismartcoding.plain.features.permissions.allGranted

private fun AppFeatureType.requiredCapability(): Capability? = when (this) {
    AppFeatureType.APPS -> Capability.PACKAGES
    AppFeatureType.NOTIFICATIONS -> Capability.NOTIFICATIONS
    AppFeatureType.CHECK_UPDATES -> Capability.APP_UPDATES
    else -> null
}

fun AppFeatureType.has(): Boolean = has(getDeviceCapabilities())

internal fun AppFeatureType.has(capabilities: Collection<Capability>, debug: Boolean = isDebugBuild()): Boolean =
    if (this == AppFeatureType.IMAGE_EDITOR) debug else requiredCapability()?.let { it in capabilities } ?: true

fun AppFeatureType.hasPermission(): Boolean = getPermission()?.let { allGranted(it.permissions) } ?: true
