package com.ismartcoding.plain.db

import androidx.compose.runtime.Composable
import androidx.room3.ColumnInfo
import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Insert
import androidx.room3.PrimaryKey
import androidx.room3.Query
import androidx.room3.Update
import androidx.room3.Upsert
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
) : DEntityBase() {
    fun isPaired(): Boolean = status == PeerStatus.PAIRED
    fun getIpList(): List<String> {
        return ip.split(",").map { it.trim() }.filter { it.isNotEmpty() }
    }
}

@Dao
interface PeerDao {
    @Query("SELECT * FROM peers")
    suspend fun getAll(): List<DPeer>

    @Query("SELECT * FROM peers where status = 'PAIRED'")
    suspend fun getAllPaired(): List<DPeer>

    @Query("SELECT * FROM peers where status IN ('PAIRED', 'CHANNEL')")
    suspend fun getAllWithPublicKey(): List<DPeer>

    @Query("SELECT * FROM peers WHERE id = :id")
    suspend fun getById(id: String): DPeer?

    @Query("SELECT * FROM peers WHERE id IN (:ids)")
    suspend fun getByIds(ids: List<String>): List<DPeer>

    @Insert
    suspend fun insert(vararg item: DPeer)

    @Update
    suspend fun update(vararg item: DPeer)

    @Upsert
    suspend fun upsert(vararg item: DPeer)

    @Query("DELETE FROM peers WHERE id = :id")
    suspend fun delete(id: String)

    @Query("DELETE FROM peers WHERE id in (:ids)")
    suspend fun deleteByIds(ids: List<String>)
}
