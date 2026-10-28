package com.ismartcoding.plain.models

import com.ismartcoding.plain.platform.system.Capability
import kotlinx.serialization.Serializable

@Serializable
data class ManagedDeviceInfo(
    val deviceName: String,
    val capabilities: List<Capability>,
    val appDir: String,
    val downloadsDir: String,
    val urlToken: String,
)

@Serializable
data class ManagedStorageRoot(val name: String, val path: String)
