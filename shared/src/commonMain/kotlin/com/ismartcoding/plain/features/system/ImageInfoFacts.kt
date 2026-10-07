package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class ImageInfoFacts(
    val width: Int,
    val height: Int,
    val location: LocationFacts?,
)
