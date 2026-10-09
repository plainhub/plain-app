package com.ismartcoding.plain.db

import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant
import com.ismartcoding.plain.lib.generateId

data class DPomodoroItem(
    override var id: String = generateId(),

    var date: String = "", // YYYY-MM-DD format

    var completedCount: Int = 0,

    var totalWorkSeconds: Int = 0,

    var totalBreakSeconds: Int = 0,

    var createdAt: Instant = TimeHelper.now(),

    var updatedAt: Instant = TimeHelper.now(),
) : IData
