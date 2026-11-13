package com.ismartcoding.plain.features.nearby

import com.ismartcoding.plain.models.NearbyDevice
import kotlinx.serialization.Serializable

@Serializable
data class NearbyDiscoverySnapshot(
    val isDiscovering: Boolean,
    val nearbyDevices: List<NearbyDevice>,
)
