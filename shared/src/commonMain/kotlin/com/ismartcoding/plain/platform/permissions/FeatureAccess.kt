package com.ismartcoding.plain.platform.permissions

import com.ismartcoding.plain.features.web.AccessFeature

expect fun AccessFeature.systemPermissions(): Set<Permission>
fun AccessFeature.isGranted(): Boolean = systemPermissions().all { it.isGranted() }

internal expect suspend fun Set<AccessFeature>.requestSystemAccess()
