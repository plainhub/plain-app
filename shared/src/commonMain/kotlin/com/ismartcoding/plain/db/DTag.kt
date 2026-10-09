package com.ismartcoding.plain.db

import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant
import com.ismartcoding.plain.lib.generateId

data class DTag(
    override var id: String = generateId(),
    var name: String = "",
    var type: Int = 0,
    var count: Int = 0,

    var createdAt: Instant = TimeHelper.now(),

    var updatedAt: Instant = TimeHelper.now(),
) : IData
