package com.ismartcoding.plain.ui.models

import com.ismartcoding.plain.data.DCertificate
import com.ismartcoding.plain.data.IData
import com.ismartcoding.plain.enums.PackageType
import kotlin.time.Instant

data class VPackage(
    override var id: String,
    val name: String,
    val type: PackageType,
    val version: String,
    val path: String,
    val size: Long,
    val certs: List<DCertificate>,
    val installedAt: Instant,
    val updatedAt: Instant,
) : IData {
    companion object
}
