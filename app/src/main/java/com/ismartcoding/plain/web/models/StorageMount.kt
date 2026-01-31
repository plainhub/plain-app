package com.ismartcoding.plain.web.models

data class StorageMount(
    val id: String = "",
    val name: String = "",
    val path: String = "",
    val mountPoint: String = "",
    val fsType: String = "",
    val totalBytes: Long = 0L,
    val usedBytes: Long = 0L,
    val freeBytes: Long = 0L,
    val remote: Boolean = false,
    val alias: String = "",
    val driveType: String = "",
    val diskID: String = "",
)
