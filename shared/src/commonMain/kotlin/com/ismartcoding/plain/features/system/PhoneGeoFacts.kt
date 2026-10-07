package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class PhoneGeoFacts(
    val country: String,
    val numberType: String,
    val carrier: String,
    val description: String,
)
