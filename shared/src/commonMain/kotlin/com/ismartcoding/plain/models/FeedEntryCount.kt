package com.ismartcoding.plain.models

import kotlinx.serialization.Serializable

@Serializable
data class FeedEntryCount(
    var feedId: String,
    var count: Int,
)
