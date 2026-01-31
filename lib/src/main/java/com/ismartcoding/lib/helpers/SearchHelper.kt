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
    var FILTER_DELIMITER = ":"
    const val NOT_TYPE = "NOT"

    private val INVERT = mapOf(
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
    private val GROUP_TYPES = INVERT.keys.filter { it != "in" && it != "nin" }

    private fun splitInGroup(input: String): List<String> {
        val result = mutableListOf<String>()
        val sb = StringBuilder()
        var quoteChar: Char? = null
        var escape = false

        for (c in input) {
            when {
                escape -> {
                    sb.append(c)
                    escape = false
                }
                c == '\\' -> {
                    escape = true
                }
                quoteChar != null -> {
                    if (c == quoteChar) {
                        quoteChar = null
                    } else {
                        sb.append(c)
                    }
                }
                c == '"' || c == '\'' -> {
                    quoteChar = c
                }
                c.isWhitespace() -> {
                    if (sb.isNotEmpty()) {
                        result.add(sb.toString())
                        sb.clear()
                    }
                }
                else -> {
                    sb.append(c)
                }
            }
        }
        if (sb.isNotEmpty()) {
            result.add(sb.toString())
        }

        return result
    }

    // 仅移除首尾的匹配引号
    private fun removeQuotation(s: String): String {
        return if ((s.startsWith('"') && s.endsWith('"')) || (s.startsWith('\'') && s.endsWith('\''))) {
            s.substring(1, s.length - 1)
        } else {
            s
        }
    }

    private fun detectGroupType(group: String): String {
        // Match by prefix and prefer longer operators first.
        // Example: ">=60" must match ">=" (not "=").
        return GROUP_TYPES
            .sortedByDescending { it.length }
            .find { group.startsWith(it) } ?: ""
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
        if (q.isEmpty()) return emptyList()

        val groups = splitInGroup(q).map { parseGroup(it) }

        var invert = false
        groups.forEach {
            if (it.op == NOT_TYPE) {
                invert = true
            } else if (invert) {
                it.op = INVERT[it.op] ?: it.op
                invert = false
            }
        }

        return groups.filter { it.op != NOT_TYPE }
    }
}