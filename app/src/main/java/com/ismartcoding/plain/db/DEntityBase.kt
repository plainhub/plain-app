package com.ismartcoding.plain.db

import androidx.room.ColumnInfo
import com.ismartcoding.plain.helpers.TimeHelper
import kotlinx.datetime.Instant

abstract class DEntityBase {
    @ColumnInfo(name = "created_at")
    var createdAt: Instant = TimeHelper.now()

    @ColumnInfo(name = "updated_at")
    var updatedAt: Instant = TimeHelper.now()
}
