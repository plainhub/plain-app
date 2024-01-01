package com.ismartcoding.plain.features.pkg

import android.content.pm.ApplicationInfo
import kotlinx.datetime.Instant

data class DPackage(
    val app: ApplicationInfo,
    val id: String,
    val name: String,
    val type: String,
    val version: String,
    val path: String,
    val size: Long,
    val certs: List<DCertificate>,
    val installedAt: Instant,
    val updatedAt: Instant,
)

data class DCertificate(val issuer: String, val subject: String, val serialNumber: String, val validFrom: Instant, val validTo: Instant)
