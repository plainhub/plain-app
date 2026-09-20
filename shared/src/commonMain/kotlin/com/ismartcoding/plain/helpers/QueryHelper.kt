package com.ismartcoding.plain.helpers

import com.ismartcoding.plain.features.TagHelper
import com.ismartcoding.plain.lib.kgraphql.GraphQLError

object QueryHelper {
    /** Sentinel field name for explicitly targeting the whole table: `all:true` (API_SPEC §5). */
    const val BULK_ALL_FIELD = "all"

    /**
     * Destructive bulk mutations must address an explicit target: a blank query would fall
     * through the empty-where `1=1` path and silently hit the whole table. Whole-table
     * intent is expressed with the `all:true` sentinel instead.
     */
    fun requireExplicitBulkQuery(query: String) {
        if (query.isBlank()) {
            throw GraphQLError(
                "query is required for bulk mutations — pass 'all:true' to explicitly target everything (API_SPEC §5)",
            )
        }
    }

    /** Text-search term of a query DSL string ("" when absent) — used by list endpoints that only support text filtering. */
    fun textOf(query: String): String = SearchHelper.parse(query).firstOrNull { it.name == "text" }?.value ?: ""

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
