package com.ismartcoding.lib.kgraphql.request

/**
 * Raw reference to type, e.g. in query variables section '($var: [String!]!)', '[String!]!' is type reference
 */
data class TypeReference (
        val name: String,
        val isNullable: Boolean = false,
        val isList : Boolean = false,
        val isElementNullable: Boolean = isList
) {
    override fun toString(): String {
        return buildString {
            if (isList){
                append("[").append(name)
                if(!isElementNullable) append("!")
                append("]")
            } else {
                append(name)
            }
            if(!isNullable) append("!")
        }
    }
}
