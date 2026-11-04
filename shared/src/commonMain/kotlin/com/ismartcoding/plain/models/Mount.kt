package com.ismartcoding.plain.models

import com.ismartcoding.plain.features.files.DriveType
import kotlinx.serialization.Serializable

@Serializable
data class Mount(
    var id: String,
    var name: String,
    var path: String,
    var mountPoint: String,
    var fsType: String,
    var totalBytes: Long,
    var usedBytes: Long,
    var freeBytes: Long,
    var remote: Boolean,
    var alias: String,
    var driveType: DriveType,
    var diskId: String,
    var label: String,
    var diskModel: String,
)
