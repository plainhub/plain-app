package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class LocationFacts(
    val latitude: Double,
    val longitude: Double,
)
