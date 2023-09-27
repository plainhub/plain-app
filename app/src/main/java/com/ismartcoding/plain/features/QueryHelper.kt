package com.ismartcoding.plain.features

import com.ismartcoding.lib.helpers.CoroutinesHelper.withIO
import com.ismartcoding.lib.helpers.SearchHelper
import com.ismartcoding.plain.features.tag.TagHelper

object QueryHelper {
    suspend fun prepareQuery(query: String): String {
        var newQuery = query
        if (query.isNotEmpty()) {
            val queryGroups = SearchHelper.parse(query)
            val tagIds = queryGroups.filter { it.name == "tag_id" }.map { it.value }.toSet()
            if (tagIds.isNotEmpty()) {
                val ids = withIO { TagHelper.getKeysByTagIds(tagIds) }
                newQuery += " ids:${ids.joinToString(",")}"
            }
        }

        return newQuery
    }
}
