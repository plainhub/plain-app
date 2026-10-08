package com.ismartcoding.plain.features.call

import kotlinx.serialization.Serializable

@Serializable
internal data class PhoneMetadataRequest(val countryCode: Int, val nationalNumber: String, val locale: String, val includeCarrier: Boolean)
