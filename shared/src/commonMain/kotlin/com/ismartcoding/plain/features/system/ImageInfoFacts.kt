package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
data class ImageInfoFacts(
    val width: Int,
    val height: Int,
    val location: LocationFacts?,
)
