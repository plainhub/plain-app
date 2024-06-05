package com.ismartcoding.plain.helpers

import com.ismartcoding.lib.helpers.FilterField
import com.ismartcoding.lib.helpers.SearchHelper
import com.ismartcoding.plain.features.TagHelper

object QueryHelper {
    suspend fun parseAsync(query: String): List<FilterField>  {
        if (query.isNotEmpty()) {
            val fields = SearchHelper.parse(query).toMutableList()
            val tagIds = fields.filter { it.name == "tag_id" }.map { it.value }.toSet()
            if (tagIds.isNotEmpty()) {
                val ids = TagHelper.getKeysByTagIdsAsync(tagIds)
                fields.removeIf { it.name == "tag_id" }
                if (ids.isNotEmpty()) {
                    fields.add(FilterField("ids", ":", ids.joinToString(",")))
                } else {
                    fields.add(FilterField("ids", ":","invalid_ids")) // still need to set the ids to indicate no result
                }
            }
            return fields
        }

        return emptyList()
    }
}
