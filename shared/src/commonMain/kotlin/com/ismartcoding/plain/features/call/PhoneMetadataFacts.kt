package com.ismartcoding.plain.features.call

import kotlinx.serialization.Serializable

@Serializable
internal data class PhoneMetadataFacts(val carrier: String, val description: String)
