package com.ismartcoding.plain.platform

import com.ismartcoding.plain.api.ApiResult
import com.ismartcoding.plain.db.DFeedEntry
import com.ismartcoding.plain.features.feed.FeedEntryHelper
import kotlinx.coroutines.CancellationException

suspend fun DFeedEntry.fetchContentAsync(): ApiResult = try {
    val refreshed = FeedEntryHelper.syncContent(id)
    content = refreshed.content
    image = refreshed.image
    updatedAt = refreshed.updatedAt
    ApiResult(null, success = true)
} catch (cancelled: CancellationException) { throw cancelled }
catch (error: Exception) { ApiResult(null, error) }
