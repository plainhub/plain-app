package com.ismartcoding.plain.models

import com.ismartcoding.plain.platform.system.Capability
import kotlinx.serialization.Serializable

@Serializable
data class ManagedDeviceInfo(
    val deviceName: String,
    val capabilities: List<Capability>,
    val permissions: List<com.ismartcoding.plain.platform.permissions.Permission>,
    val appDir: String,
    val downloadsDir: String,
    val urlToken: String,
    val tlsCertificateSha256: String?,
)


@Serializable
data class ManagedStorageRoot(val name: String, val path: String)

data class ManagedResource(val url: String, val tlsCertificateSha256: String)
