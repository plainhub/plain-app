package com.ismartcoding.plain.db

import androidx.room.ColumnInfo
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

abstract class DEntityBase {
    @ColumnInfo(name = "created_at")
    var createdAt: Instant = Clock.System.now()

    @ColumnInfo(name = "updated_at")
    var updatedAt: Instant = Clock.System.now()
}
