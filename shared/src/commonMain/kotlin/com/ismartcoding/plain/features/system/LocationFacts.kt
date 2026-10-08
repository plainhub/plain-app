package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
data class LocationFacts(
    val latitude: Double,
    val longitude: Double,
)
