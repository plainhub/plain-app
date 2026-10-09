package com.ismartcoding.plain.db

import androidx.compose.runtime.Composable
import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.enums.PeerStatus

data class DPeer(
    var id: String,
    var name: String = "",
    var ip: String = "",
    var key: String = "",
    var publicKey: String = "",
    var status: PeerStatus = PeerStatus.UNPAIRED,
    var port: Int = 0,
    var deviceType: DeviceType = DeviceType.PHONE,

    var createdAt: Instant = TimeHelper.now(),
    var updatedAt: Instant = TimeHelper.now(),
    var address: PeerAddress? = null,
) {
    fun isPaired(): Boolean = status == PeerStatus.PAIRED
    fun getIpList(): List<String> {
        return ip.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
