package com.ismartcoding.plain.data

enum class SortDirection {
    ASC,
    DESC,
}

data class SortBy(val field: String, val direction: SortDirection)