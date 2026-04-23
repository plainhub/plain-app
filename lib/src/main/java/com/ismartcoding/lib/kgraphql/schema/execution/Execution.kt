package com.ismartcoding.lib.kgraphql.schema.execution

import com.ismartcoding.lib.kgraphql.schema.directive.Directive
import com.ismartcoding.lib.kgraphql.schema.introspection.NotIntrospected
import com.ismartcoding.lib.kgraphql.schema.structure.Field
import com.ismartcoding.lib.kgraphql.schema.structure.Type
import com.ismartcoding.lib.kgraphql.schema.model.ast.ArgumentNodes
import com.ismartcoding.lib.kgraphql.schema.model.ast.SelectionNode
import com.ismartcoding.lib.kgraphql.schema.model.ast.VariableDefinitionNode

sealed class Execution {
    abstract val selectionNode: SelectionNode
    abstract val directives: Map<Directive, ArgumentNodes?>?

    @NotIntrospected
    open class Node (
        override val selectionNode: SelectionNode,
        val field: Field,
        val children: Collection<Execution>,
        val key : String,
        val alias: String? = null,
        val arguments : ArgumentNodes? = null,
        val typeCondition: TypeCondition? = null,
        override val directives: Map<Directive, ArgumentNodes?>? = null,
        val variables: List<VariableDefinitionNode>? = null
    ) : Execution() {
        val aliasOrKey = alias ?: key
    }

    class Fragment(
        override val selectionNode: SelectionNode,
        val condition: TypeCondition,
        val elements : List<Execution>,
        override val directives: Map<Directive, ArgumentNodes?>?
    ) : Execution()

    class Union (
        node: SelectionNode,
        val unionField: Field.Union<*>,
        val memberChildren: Map<Type, Collection<Execution>>,
        key: String,
        alias: String? = null,
        condition : TypeCondition? = null,
        directives: Map<Directive, ArgumentNodes?>? = null
    ) : Node (
        selectionNode = node,
        field = unionField,
        children = emptyList(),
        key = key,
        alias = alias,
        typeCondition = condition,
        directives = directives
    ) {
        fun memberExecution(type: Type): Node {
            return Node (
                selectionNode = selectionNode,
                field = field,
                children = memberChildren[type] ?: throw IllegalArgumentException("Union ${unionField.name} has no member $type"),
                key = key,
                alias = alias,
                arguments = arguments,
                typeCondition = typeCondition,
                directives = directives,
                variables = null
            )
        }
    }
}
