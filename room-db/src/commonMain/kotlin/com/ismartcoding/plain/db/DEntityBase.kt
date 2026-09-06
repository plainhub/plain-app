package com.ismartcoding.plain.db

import androidx.room3.ColumnInfo
import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant

open class DEntityBase {
    @ColumnInfo(name = "created_at")
    var createdAt: Instant = TimeHelper.now()

    @ColumnInfo(name = "updated_at")
    var updatedAt: Instant = TimeHelper.now()
}
