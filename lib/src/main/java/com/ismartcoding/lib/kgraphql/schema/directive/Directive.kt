package com.ismartcoding.lib.kgraphql.schema.directive

import com.ismartcoding.lib.kgraphql.schema.directive.DirectiveLocation.FIELD
import com.ismartcoding.lib.kgraphql.schema.directive.DirectiveLocation.FRAGMENT_SPREAD
import com.ismartcoding.lib.kgraphql.schema.directive.DirectiveLocation.INLINE_FRAGMENT
import com.ismartcoding.lib.kgraphql.schema.introspection.__Directive
import com.ismartcoding.lib.kgraphql.schema.introspection.__InputValue
import com.ismartcoding.lib.kgraphql.schema.model.FunctionWrapper
import com.ismartcoding.lib.kgraphql.schema.structure.InputValue

/**
 * Directives provide a way to describe alternate runtime execution and type validation behavior in a GraphQL document.
 */
data class Directive (
        override val name: String,
        override val locations: List<DirectiveLocation>,
        val execution: DirectiveExecution,
        override val description: String?,
        val arguments: List<InputValue<*>>
) : __Directive {

    override val args: List<__InputValue>
        get() = arguments

    data class Partial(
            val name: String,
            val locations: List<DirectiveLocation>,
            val execution: DirectiveExecution,
            val description: String? = null
    ) {
        fun toDirective(inputValues: List<InputValue<*>>) = Directive(
                name = this.name,
                locations = this.locations,
                execution = this.execution,
                description = this.description,
                arguments = inputValues
        )
    }

    companion object {
        /**
         * The @skip directive may be provided for fields, fragment spreads, and inline fragments.
         * Allows for conditional exclusion during execution as described by the if argument.
         */
        val SKIP = Directive.Partial( "skip",
                listOf(FIELD, FRAGMENT_SPREAD, INLINE_FRAGMENT),
                DirectiveExecution(FunctionWrapper.on({ `if` : Boolean -> DirectiveResult(!`if`) }))
        )

        /**
         * The @include directive may be provided for fields, fragment spreads, and inline fragments.
         * Allows for conditional inclusion during execution as described by the if argument.
         */
        val INCLUDE = Directive.Partial( "include",
                listOf(FIELD, FRAGMENT_SPREAD, INLINE_FRAGMENT),
                DirectiveExecution(FunctionWrapper.on({ `if` : Boolean -> DirectiveResult(`if`) }))
        )
    }
}
