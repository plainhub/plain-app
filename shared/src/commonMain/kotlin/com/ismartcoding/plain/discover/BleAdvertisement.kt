package com.ismartcoding.plain.discover

import kotlinx.serialization.Serializable

@Serializable
data class BleAdvertisement(
    val shortId: String,
    val awareSupported: Boolean,
    val awareRunning: Boolean,
)
