package com.ismartcoding.plain.models

import com.ismartcoding.plain.features.nearby.PairingMode
import kotlinx.serialization.Serializable

@Serializable
data class PairPeerInput(
    val peerId: String,
    val mode: PairingMode,
    val password: String? = null,
)
