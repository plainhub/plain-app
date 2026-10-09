package com.ismartcoding.plain.db

import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant
import com.ismartcoding.plain.lib.generateId

data class DBookChapter(
    override var id: String = generateId(),
    var name: String = "",

    var bookId: String = "",

    var parentId: String = "",

    var content: String = "",

    var sortOrder: Int = 0,

    var createdAt: Instant = TimeHelper.now(),

    var updatedAt: Instant = TimeHelper.now(),
) : IData
