package com.ismartcoding.plain.models

import com.ismartcoding.plain.IData
import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Required

@Serializable
data class Bookmark(
    @Required override var id: String = "",
    @Required var url: String = "",
    @Required var title: String = "",
    @Required var faviconPath: String = "",
    @Required var groupId: String = "",
    @Required var pinned: Boolean = false,
    @Required var clickCount: Int = 0,
    @Required var lastClickedAt: Instant? = null,
    @Required var sortOrder: Int = 0,
    @Required var createdAt: Instant = TimeHelper.now(),
    @Required var updatedAt: Instant = TimeHelper.now(),
) : IData
