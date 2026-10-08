package com.ismartcoding.plain.features.call

import kotlinx.serialization.Serializable

/**
 * Lookup result. Every field is empty when the platform cannot make sense of
 * the number — an empty [country] is what marks the number as unknown.
 */
@Serializable
internal data class PhoneMetadataFacts(val country: String, val numberType: String, val carrier: String, val description: String)
