package com.ismartcoding.plain.features.system

import kotlinx.serialization.Serializable

@Serializable
internal data class CallFacts(
    val id: String,
    val number: String,
    val name: String,
    val photoId: String,
    val startedAt: String,
    val durationSec: Int,
    val type: Int,
    val accountId: String,
    val geo: PhoneGeoFacts?,
)
