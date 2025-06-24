package com.ismartcoding.plain.data

import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.enums.NearbyMessageType
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

@Serializable
@OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)
data class DDiscoverRequest(
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val fromId: String = "",    // Sender's own ID, optional
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val toId: String = ""       // If directed scan, encrypted target ID, optional
)

@Serializable
data class DDiscoverReply(
    val id: String,                // Device ID
    val name: String,              // Device name
    val port: Int,                 // HTTPS API port
    val deviceType: DeviceType,
    val version: String,
    val platform: String,
)