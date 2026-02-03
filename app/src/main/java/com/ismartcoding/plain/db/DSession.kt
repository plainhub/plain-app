package com.ismartcoding.plain.db

import androidx.room.*
import com.ismartcoding.plain.helpers.TimeHelper
import kotlinx.datetime.*

@Entity(tableName = "sessions")
data class DSession(
    @PrimaryKey
    @ColumnInfo(name = "client_id")
    var clientId: String = "",
) : DEntityBase() {
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
}

data class SessionClientTsUpdate(
    @ColumnInfo(name = "client_id")
    var clientId: String,
    @ColumnInfo(name = "updated_at")
    val updatedAt: Instant = TimeHelper.now(),
)

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions ORDER BY updated_at DESC")
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
