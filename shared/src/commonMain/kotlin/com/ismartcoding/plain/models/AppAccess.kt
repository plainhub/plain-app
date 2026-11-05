package com.ismartcoding.plain.models

import com.ismartcoding.plain.features.web.AccessFeature
import kotlinx.serialization.Serializable

@Serializable
data class AppAccess(val access: List<AccessFeature>)
