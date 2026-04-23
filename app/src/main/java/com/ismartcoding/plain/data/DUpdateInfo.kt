package com.ismartcoding.plain.data

data class DUpdateInfo(
    val newVersion: String = "",
    val checkUpdateTime: Long = 0L,
    val skipVersion: String = "",
    val publishDate: String = "",
    val log: String = "",
    val downloadUrl: String = "",
    val size: Long = 0L,
    val downloadedApkPath: String = "",
    val autoCheckUpdate: Boolean = true,
)
