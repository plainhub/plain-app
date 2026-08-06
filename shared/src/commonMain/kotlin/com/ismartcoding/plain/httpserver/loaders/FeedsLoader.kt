package com.ismartcoding.plain.httpserver.loaders

import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.feed.FeedHelper
import com.ismartcoding.plain.httpserver.models.Feed
import com.ismartcoding.plain.httpserver.models.toModel
import com.ismartcoding.plain.lib.kdataloader.ExecutionResult

object FeedsLoader {
    suspend fun load(
        ids: List<String>,
    ): List<ExecutionResult<Feed?>> {
        val map = FeedHelper.getAll().associateBy { it.id }
        return ids.map { id ->
            ExecutionResult.Success(map[id]?.toModel())
        }
    }
}
