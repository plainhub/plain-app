package com.ismartcoding.plain.models

import com.ismartcoding.plain.IData
import com.ismartcoding.plain.lib.TimeHelper
import kotlin.time.Instant
import kotlinx.serialization.Serializable
import kotlinx.serialization.Required
import kotlinx.serialization.Transient

@Serializable
data class Feed(
    @Required override var id: String = "",
    @Required var name: String = "",
    @Required var url: String = "",
    @Required var fetchContent: Boolean = false,
    @Required var logo: String = "",
    @Required var lastSyncAt: Instant? = null,
    @Required var lastError: FeedError = FeedError(),
    @Transient var entryCount: Int = 0,
    @Required var createdAt: Instant = TimeHelper.now(),
    @Required var updatedAt: Instant = TimeHelper.now(),
) : IData {
    /** True when the last sync attempt failed. */
    val hasSyncError: Boolean
        get() = lastError.code != FeedErrorCode.NONE
}
