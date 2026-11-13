package com.ismartcoding.plain.models

import com.ismartcoding.plain.platform.system.DeviceType
import com.ismartcoding.plain.features.nearby.PeerStatus
import kotlin.time.Instant
import kotlinx.serialization.Serializable

@Serializable
data class Peer(
    var id: String,
    var name: String,
    var ip: String,
    var key: String = "",
    var publicKey: String = "",
    var status: PeerStatus,
    var port: Int?,
    var deviceType: DeviceType,
    var createdAt: Instant,
    var updatedAt: Instant,
    var token: String = "",
    var online: Boolean = false,
    var managed: Boolean = false,
) {
    fun isPaired(): Boolean = status == PeerStatus.PAIRED
}
