package com.ismartcoding.plain.db

import com.ismartcoding.plain.lib.TimeHelper
import com.ismartcoding.plain.lib.generateId
import kotlin.time.Instant

data class DNote(
    override var id: String = generateId(),
    var title: String = "",

    var deletedAt: Instant? = null,

    var content: String = "",

    var createdAt: Instant = TimeHelper.now(),

    var updatedAt: Instant = TimeHelper.now(),
) : IData {
    fun getSummary(): String {
        return content.replace("\n", "").replaceFirst("^\\s*".toRegex(), "")
    }
}
