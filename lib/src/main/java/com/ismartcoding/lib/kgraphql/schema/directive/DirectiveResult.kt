package com.ismartcoding.lib.kgraphql.schema.directive

/**
 * right now its only wrapper for [include], but GraphQL creators foreshadowed that
 * directives probably will be more important and powerful in future
 */
data class DirectiveResult(val include: Boolean = true)