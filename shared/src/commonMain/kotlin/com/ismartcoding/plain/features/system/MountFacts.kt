package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable
import com.ismartcoding.plain.enums.DriveType

@Serializable
internal data class MountFacts(val name: String, val path: String, val totalBytes: Long, val freeBytes: Long, val driveType: DriveType)
