package com.ismartcoding.plain.db

import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Upsert
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant

/**
 * Persistent history of LAN-discovered devices, so the nearby page renders
 * instantly from cache instead of waiting for a fresh mDNS sweep. Rows are
 * refreshed on every discovery; a row is deleted only once a liveness probe
 * confirms the device left the LAN.
 */
@Entity(tableName = "nearby_device_cache")
data class DNearbyDeviceCache(
    @PrimaryKey var id: String,
    @ColumnInfo(name = "name") var name: String = "",
    /** Comma-joined address list, same encoding as [DPeer.ip]. */
    @ColumnInfo(name = "ips") var ips: String = "",
    @ColumnInfo(name = "port") var port: Int = 0,
    @ColumnInfo(name = "device_type") var deviceType: DeviceType = DeviceType.PHONE,
    @ColumnInfo(name = "version") var version: String = "",
    @ColumnInfo(name = "platform") var platform: String = "",
    @ColumnInfo(name = "last_seen") var lastSeen: Instant = TimeHelper.now(),
)

@Dao
interface NearbyDeviceCacheDao {
    @Query("SELECT * FROM nearby_device_cache ORDER BY last_seen DESC")
    suspend fun getAll(): List<DNearbyDeviceCache>

    @Upsert
    suspend fun upsert(vararg item: DNearbyDeviceCache)

    @Query("UPDATE nearby_device_cache SET last_seen = :lastSeen WHERE id = :id")
    suspend fun touch(id: String, lastSeen: Instant)

    @Query("DELETE FROM nearby_device_cache WHERE id = :id")
    suspend fun delete(id: String)
}
