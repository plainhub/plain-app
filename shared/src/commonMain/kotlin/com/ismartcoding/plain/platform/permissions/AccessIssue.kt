package com.ismartcoding.plain.platform.permissions

import com.ismartcoding.plain.features.web.AccessFeature
import org.jetbrains.compose.resources.StringResource

internal enum class AccessAction { REQUEST, SETTINGS, FOLDER }

internal data class AccessIssue(
    val description: StringResource,
    val features: Set<AccessFeature>,
    val action: AccessAction? = null,
    val actionText: StringResource? = null,
    val isError: Boolean = true,
)

internal expect fun Set<AccessFeature>.accessIssues(): List<AccessIssue>
