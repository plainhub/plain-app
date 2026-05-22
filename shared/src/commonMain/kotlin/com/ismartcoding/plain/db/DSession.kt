package com.ismartcoding.plain.db

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Update
import com.ismartcoding.plain.helpers.TimeHelper
import kotlin.time.Instant

@Entity(tableName = "sessions")
data class DSession(
    @PrimaryKey
    @ColumnInfo(name = "client_id")
    var clientId: String = "",
) : DEntityBase() {
    @ColumnInfo(name = "name", defaultValue = "")
    var name: String = ""

    @ColumnInfo(name = "type", defaultValue = "web")
    var type: String = TYPE_WEB

    @ColumnInfo(name = "client_ip")
    var clientIP: String = ""

    @ColumnInfo(name = "os_name")
    var osName: String = ""

    @ColumnInfo(name = "os_version")
    var osVersion: String = ""

    @ColumnInfo(name = "browser_name")
    var browserName: String = ""

    @ColumnInfo(name = "browser_version")
    var browserVersion: String = ""

    @ColumnInfo(name = "token")
    var token: String = ""

    @ColumnInfo(name = "last_active_at")
    var lastActiveAt: Instant? = null

    companion object {
        const val TYPE_WEB = "web"
        const val TYPE_CUSTOM = "custom"
    }
}

data class SessionClientTsUpdate(
    @ColumnInfo(name = "client_id")
    var clientId: String,
    @ColumnInfo(name = "last_active_at")
    val lastActiveAt: Instant = TimeHelper.now(),
)

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY last_active_at DESC")
    fun getAll(): List<DSession>

    @Query("SELECT * FROM sessions WHERE client_id=:clientId")
    fun getByClientId(clientId: String): DSession?

    @Insert
    fun insert(vararg item: DSession)

    @Update
    fun update(vararg item: DSession)

    @Update(entity = DSession::class)
    fun updateTs(items: List<SessionClientTsUpdate>)

    @Query("DELETE FROM sessions WHERE client_id=:clientId")
    fun delete(clientId: String)
}
