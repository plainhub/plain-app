package com.ismartcoding.plain.db

import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant
import com.ismartcoding.plain.lib.generateId

data class DBook(
    override var id: String = generateId(),
    var name: String = "",
    var author: String = "",
    var image: String = "",
    var description: String = "",

    var createdAt: Instant = TimeHelper.now(),

    var updatedAt: Instant = TimeHelper.now(),
) : IData
