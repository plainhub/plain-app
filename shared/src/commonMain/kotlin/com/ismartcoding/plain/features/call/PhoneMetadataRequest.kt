package com.ismartcoding.plain.features.call

import kotlinx.serialization.Serializable

/**
 * Number lookup input. The number is passed as the user has it — parsing,
 * validation and classification belong to the platform side, which owns the
 * libphonenumber data for the current region.
 */
@Serializable
internal data class PhoneMetadataRequest(val number: String, val region: String, val locale: String)
