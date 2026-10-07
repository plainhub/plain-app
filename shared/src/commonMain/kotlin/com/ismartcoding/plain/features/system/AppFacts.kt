package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class AppFacts(
    val clientId: String,
    val urlToken: String,
    val httpPort: Int,
    val httpsPort: Int,
    val appDir: String,
    val deviceName: String,
    val deviceType: String,
    val capabilities: List<String>,
    val buildChannel: String,
    val permissions: List<String>,
    val downloadsDir: String,
    val developerMode: Boolean,
    val debug: Boolean,
)
