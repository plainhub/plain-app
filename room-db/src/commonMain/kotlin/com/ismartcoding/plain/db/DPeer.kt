package com.ismartcoding.plain.db

import androidx.compose.runtime.Composable
import androidx.room3.ColumnInfo
import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant
import androidx.room3.Entity
import androidx.room3.Ignore
import androidx.room3.PrimaryKey
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.enums.PeerStatus

@Entity(tableName = "peers")
data class DPeer(
    @PrimaryKey var id: String,
    @ColumnInfo(name = "name") var name: String = "",
    @ColumnInfo(name = "ip") var ip: String = "",
    @ColumnInfo(name = "key") var key: String = "",
    @ColumnInfo(name = "public_key") var publicKey: String = "",
    @ColumnInfo(name = "status") var status: PeerStatus = PeerStatus.UNPAIRED,
    @ColumnInfo(name = "port") var port: Int = 0,
    @ColumnInfo(name = "device_type") var deviceType: DeviceType = DeviceType.PHONE,

    @ColumnInfo(name = "created_at") var createdAt: Instant = TimeHelper.now(),
    @ColumnInfo(name = "updated_at") var updatedAt: Instant = TimeHelper.now(),
    @Ignore var address: PeerAddress? = null,
) {
    fun isPaired(): Boolean = status == PeerStatus.PAIRED
    fun getIpList(): List<String> {
        return ip.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}
