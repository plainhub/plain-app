package com.ismartcoding.plain.preferences

import kotlinx.serialization.Serializable

@Serializable
data class UpdateInfoPatch(
    val newVersion: String? = null,
    val checkUpdateTime: Long? = null,
    val skipVersion: String? = null,
    val publishDate: String? = null,
    val log: String? = null,
    val downloadUrl: String? = null,
    val size: Long? = null,
    val downloadedApkPath: String? = null,
    val autoCheckUpdate: Boolean? = null,
)
