package com.ismartcoding.plain.platform.system

import com.ismartcoding.plain.features.permissions.allGranted
import com.ismartcoding.plain.platform.system.Capability
import com.ismartcoding.plain.platform.system.getDeviceCapabilities

private fun AppFeatureType.requiredCapability(): Capability? = when (this) {
        AppFeatureType.FILES -> Capability.FILE_BROWSER
        AppFeatureType.IMAGES -> Capability.IMAGE_LIBRARY
        AppFeatureType.AUDIO -> Capability.AUDIO_LIBRARY
        AppFeatureType.VIDEOS -> Capability.VIDEO_LIBRARY
        AppFeatureType.DOCS -> Capability.DOCUMENT_LIBRARY
        AppFeatureType.SOUND_METER -> Capability.SOUND_METER
        AppFeatureType.APPS -> Capability.PACKAGES
        AppFeatureType.SMS -> Capability.SMS
        AppFeatureType.CALLS -> Capability.CALLS
        AppFeatureType.CONTACTS -> Capability.CONTACTS
        AppFeatureType.NOTIFICATIONS -> Capability.NOTIFICATIONS
        AppFeatureType.MIRROR_AUDIO -> Capability.MIRROR_AUDIO
        AppFeatureType.IMAGE_EDITOR -> Capability.IMAGE_EDITOR
        AppFeatureType.NOTES -> Capability.NOTES
        AppFeatureType.FEEDS -> Capability.FEEDS
        AppFeatureType.POMODORO_TIMER -> Capability.POMODORO
        AppFeatureType.CHECK_UPDATES -> Capability.APP_UPDATES
        AppFeatureType.DONATION -> Capability.DONATION
        else -> null
    }

fun AppFeatureType.has(): Boolean = requiredCapability()?.let { it in getDeviceCapabilities() } ?: true

internal fun AppFeatureType.has(capabilities: Collection<Capability>): Boolean = requiredCapability()?.let { it in capabilities } ?: true

fun AppFeatureType.hasPermission(): Boolean = getPermission()?.let { allGranted(it.permissions) } ?: true
