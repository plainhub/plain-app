package com.ismartcoding.plain.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow
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

    @Query("UPDATE sessions SET updated_at=:updatedAt WHERE client_id=:clientId")
    fun updateTs(clientId: String, updatedAt: Instant)

    @Query("DELETE FROM sessions WHERE client_id=:clientId")
    fun delete(clientId: String)
}