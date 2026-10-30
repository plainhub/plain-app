package com.ismartcoding.plain.rust.graphql

internal fun selectionQuery(ids: Collection<String>): String {
    require(ids.isNotEmpty()) { "Selection required" }
    require(ids.all { id -> id.isNotBlank() && ',' !in id }) { "Invalid ID list" }

    val joinedIds = ids.distinct().joinToString(",")
    return "ids:=${escapeQueryValue(joinedIds)}"
}

internal fun escapeQueryValue(value: String): String = buildString {
    for (character in value) {
        if (character == '\\' || character == '\'' || character == '"' || character.isWhitespace()) {
            append('\\')
        }
        append(character)
    }
}
