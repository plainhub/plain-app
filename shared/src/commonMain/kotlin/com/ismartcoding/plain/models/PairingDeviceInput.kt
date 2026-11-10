package com.ismartcoding.plain.models

import com.ismartcoding.plain.features.nearby.PairingMode
import kotlinx.serialization.Serializable

@Serializable
data class PairingDeviceInput(
    val id: String? = null,
    val host: String? = null,
    val mode: PairingMode? = null,
    val password: String? = null,
    val generation: String? = null,
    val confirm: Boolean? = null,
)
