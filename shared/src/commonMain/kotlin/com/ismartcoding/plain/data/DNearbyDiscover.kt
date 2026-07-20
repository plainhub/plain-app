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
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val ips: List<String> = emptyList(), // All IP addresses of the device
    // Aware flags are also broadcast in the BLE scan response serviceData
    // (byte[0]) so peers can read them without a GATT connection (used by
    // PeerTransportPrewarmer). The values here are the authoritative source —
    // they overwrite the scan-level hint after a full GATT DISCOVER.
    // Older peers that don't include these fields default to false.
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val awareSupported: Boolean = false,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val awareRunning: Boolean = false,
)