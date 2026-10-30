package com.ismartcoding.plain.models

import com.ismartcoding.plain.platform.permissions.Permission
import kotlinx.serialization.Serializable

@Serializable
data class AppPermissions(val permissions: List<Permission>)
