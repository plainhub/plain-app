package com.ismartcoding.lib.kgraphql.schema.directive


enum class DirectiveLocation {
    QUERY,
    MUTATION,
    SUBSCRIPTION,
    FIELD,
    FRAGMENT_DEFINITION,
    FRAGMENT_SPREAD,
    INLINE_FRAGMENT;

    companion object {
        fun from(str: String) = str.lowercase().let { lowered ->
            values().firstOrNull { it.name.lowercase() == lowered }
        }
    }
}
