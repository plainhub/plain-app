package com.ismartcoding.lib.content

data class ContentWhere(private val selections: MutableList<String> = mutableListOf(), val args: MutableList<String> = mutableListOf()) {
    fun addIn(
        field: String,
        values: List<String>,
    ) {
        if (values.isNotEmpty()) {
            selections.add("$field IN (${CharArray(values.size) { '?' }.joinToString(",")})")
            args.addAll(values)
        }
    }

    fun add(
        selection: String,
        value: String? = null,
    ) {
        selections.add(selection)
        if (value != null) {
            args.add(value)
        }
    }

    fun addEqual(
        field: String,
        value: String,
    ) {
        add("$field = ?", value)
    }

    fun addLike(
        field: String,
        value: String,
    ) {
        add("$field LIKE '%' || ? || '%'", value)
    }

    fun addLikes(
        fields: List<String>,
        values: List<String>,
    ) {
        val r =
            fields.joinToString(" OR ") {
                "$it LIKE '%' || ? || '%'"
            }
        selections.add("($r)")
        args.addAll(values)
    }

    fun toSelection(): String {
        if (selections.isEmpty()) {
            return "1=1"
        }
        return selections.joinToString(" AND ")
    }
}
