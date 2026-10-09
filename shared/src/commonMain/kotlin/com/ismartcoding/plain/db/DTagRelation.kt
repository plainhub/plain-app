package com.ismartcoding.plain.db

import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant

data class DTagRelation(
    var tagId: String = "",
    var key: String = "",
    var type: Int = 0,

    var createdAt: Instant = TimeHelper.now(),
    var size: Long = 0,
    var title: String = "",
)

data class DTagCount(var id: String, var count: Int)
