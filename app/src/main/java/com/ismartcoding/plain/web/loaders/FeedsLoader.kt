package com.ismartcoding.plain.web.loaders

import com.ismartcoding.plain.enums.DataType
import com.ismartcoding.plain.features.feed.FeedHelper
import com.ismartcoding.plain.web.models.Feed
import com.ismartcoding.plain.web.models.toModel
import nidomiro.kdataloader.ExecutionResult

object FeedsLoader {
    fun load(
        ids: List<String>,
    ): List<ExecutionResult<Feed?>> {
        val map = FeedHelper.getAll().associateBy { it.id }
        return ids.map { id ->
            ExecutionResult.Success(map[id]?.toModel())
        }
    }
}
