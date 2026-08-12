package com.ismartcoding.plain.httpserver.models

import com.ismartcoding.plain.enums.DriveType
import com.ismartcoding.plain.lib.kgraphql.annotations.GraphQLType

@GraphQLType
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
    val driveType: DriveType = DriveType.INTERNAL_STORAGE,
    val diskID: String = "",
)
