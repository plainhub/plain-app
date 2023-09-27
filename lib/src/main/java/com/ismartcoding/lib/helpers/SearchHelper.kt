package com.ismartcoding.lib.helpers

internal data class QueryGroup(
    var length: Int = 0,
    var field: String = "",
    var query: String = "",
    var op: String = "",
    var value: String = "",
)

data class FilterField(
    var name: String = "",
    var op: String = "",
    var value: String = "",
)

object SearchHelper {
    private val GROUP_DELIMITER = "(?:[^\\s\"]+|\"[^\"]*\")+".toRegex()
    var FILTER_DELIMITER = ":"
    const val NOT_TYPE = "NOT"
    private val INVERT =
        mapOf(
            "=" to "!=",
            ">=" to "<",
            ">" to "<=",
            "!=" to "=",
            "<=" to ">",
            "<" to ">=",
            "in" to "nin",
            "nin" to "in",
        )
    val NUMBER_OPS = setOf(">", ">=", "<", "<=")
    private var GROUP_TYPES = INVERT.keys.filter { !setOf("in", "nin").contains(it) }

    private fun splitInGroup(s: String): List<String> {
        return GROUP_DELIMITER.findAll(s).toList().map { it.value }
    }

    private fun removeQuotation(s: String): String {
        return s.replace("['\"]+".toRegex(), "")
    }

    private fun detectGroupType(group: String): String {
        return GROUP_TYPES.find { group.contains(it) } ?: ""
    }

    private fun splitGroup(q: String): QueryGroup {
        val parts = q.split(FILTER_DELIMITER)
        val field = removeQuotation(parts[0])
        val query = removeQuotation(parts.subList(1, parts.size).joinToString(FILTER_DELIMITER))
        var type = detectGroupType(query)
        val value = query.substring(type.length)
        if (type.isEmpty()) {
            type = "="
        }

        return QueryGroup(parts.size, field, query, type, value)
    }

    private fun parseGroup(group: String): FilterField {
        if (group == NOT_TYPE) {
            return FilterField(op = NOT_TYPE)
        }

        val parts = splitGroup(group)
        return if (parts.field == "is") {
            FilterField(parts.query, "", "true")
        } else if (parts.length == 1) {
            FilterField("text", "", parts.field)
        } else {
            FilterField(parts.field, parts.op, parts.value)
        }
    }

    // q = "Hello World" username:plain ids:1,2,3 stars:>10 stars:<100 NOT language:javascript
    fun parse(q: String): List<FilterField> {
        val groups =
            splitInGroup(q).map {
                parseGroup(it)
            }
        var invert = false
        groups.forEach {
            if (it.op == NOT_TYPE) {
                invert = true
            } else if (invert) {
                it.op = INVERT[it.op] ?: ""
                invert = false
            }
        }

        return groups.filter { it.op != NOT_TYPE }
    }
}
