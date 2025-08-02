package com.ismartcoding.plain.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.ismartcoding.lib.extensions.urlEncode
import com.ismartcoding.plain.R
import com.ismartcoding.plain.features.locale.LocaleHelper.getString

@Entity(tableName = "peers")
data class DPeer(
    @PrimaryKey var id: String,
) : DEntityBase() {
    @ColumnInfo(name = "name")
    var name: String = ""

    @ColumnInfo(name = "ip")
    var ip: String = ""

    @ColumnInfo(name = "key")
    var key: String = ""

    @ColumnInfo(name = "public_key")
    var publicKey: String = ""

    @ColumnInfo(name = "status")
    var status: String = "" // paired, unpaired

    @ColumnInfo(name = "port")
    var port: Int = 0

    @ColumnInfo(name = "device_type")
    var deviceType: String = "" // phone, tablet, pc, etc.

    fun getApiUrl(): String {
        return "${getBaseUrl()}/peer_graphql"
    }

    fun getBaseUrl(): String {
        return "https://${ip}:${port}"
    }

    fun getFileUrl(fileId: String): String {
        return "${getBaseUrl()}/fs?id=${fileId.urlEncode()}"
    }

    fun getStatusText(): String {
        return when (status) {
            "paired" -> getString(R.string.paired)
            "unpaired" -> getString(R.string.unpaired)
            else -> getString(R.string.unknown)
        }
    }
}

@Dao
interface PeerDao {
    @Query("SELECT * FROM peers")
    fun getAll(): List<DPeer>

    @Query("SELECT * FROM peers where status = 'paired'")
    fun getAllPaired(): List<DPeer>

    @Query("SELECT * FROM peers WHERE id = :id")
    fun getById(id: String): DPeer?

    @Insert
    fun insert(vararg item: DPeer)

    @Update
    fun update(vararg item: DPeer)

    @Query("DELETE FROM peers WHERE id = :id")
    fun delete(id: String)

    @Query("DELETE FROM peers WHERE id in (:ids)")
    fun deleteByIds(ids: List<String>)
} 