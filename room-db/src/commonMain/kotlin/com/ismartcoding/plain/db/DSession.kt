package com.ismartcoding.plain.db

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import com.ismartcoding.plain.enums.SessionType
import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant

@Entity(tableName = "sessions")
data class DSession(
    @PrimaryKey
    @ColumnInfo(name = "client_id")
    var clientId: String = "",

    @ColumnInfo(name = "name", defaultValue = "")
    var name: String = "",

    @ColumnInfo(name = "type", defaultValue = "WEB")
    var type: SessionType = SessionType.WEB,

    @ColumnInfo(name = "client_ip")
    var clientIp: String = "",

    @ColumnInfo(name = "os_name")
    var osName: String = "",

    @ColumnInfo(name = "os_version")
    var osVersion: String = "",

    @ColumnInfo(name = "browser_name")
    var browserName: String = "",

    @ColumnInfo(name = "browser_version")
    var browserVersion: String = "",

    @ColumnInfo(name = "token")
    var token: String = "",

    @ColumnInfo(name = "last_active_at")
    var lastActiveAt: Instant? = null,

    @ColumnInfo(name = "created_at")
    var createdAt: Instant = TimeHelper.now(),

    @ColumnInfo(name = "updated_at")
    var updatedAt: Instant = TimeHelper.now(),
)
