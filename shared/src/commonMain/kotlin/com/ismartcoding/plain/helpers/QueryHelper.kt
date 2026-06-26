package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.features.TagHelper

object QueryHelper {
    suspend fun parseAsync(query: String): List<FilterField>  {
        if (query.isNotEmpty()) {
            val parsed = SearchHelper.parse(query)
            val tagIds = parsed.filter { it.name == "tag_id" }.map { it.value }.toSet()
            val fields = parsed.filter { it.name != "tag_id" }.toMutableList()
            if (tagIds.isNotEmpty()) {
                val ids = TagHelper.getKeysByTagIdsAsync(tagIds)
                if (ids.isNotEmpty()) {
                    fields.add(FilterField("ids", ":", ids.joinToString(",")))
                } else {
                    fields.add(FilterField("ids", ":","invalid_ids"))
                }
            }
            return fields
        }

        return emptyList()
    }
}
