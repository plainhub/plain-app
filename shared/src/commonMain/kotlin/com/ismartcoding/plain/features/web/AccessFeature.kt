package com.ismartcoding.plain.features.web

import kotlinx.serialization.Serializable

@Serializable
enum class AccessFeature {
    IMAGES,
    VIDEOS,
    AUDIO,
    DOCS,
    FILES,
    CONTACTS,
    SMS,
    CALL_LOGS,
    CALL_PHONE,
    PHONE_NUMBER,
    APPS,
    NOTIFICATIONS,
    CLIPBOARD,
    ADB,
}
