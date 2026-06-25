package com.ismartcoding.plain.lib.kgraphql.schema.structure

import com.ismartcoding.plain.lib.kgraphql.request.VariablesJson
import com.ismartcoding.plain.lib.kgraphql.schema.DefaultSchema.Companion.OPERATION_NAME_PARAM
import com.ismartcoding.plain.lib.kgraphql.schema.directive.Directive
import com.ismartcoding.plain.lib.kgraphql.schema.execution.Execution
import com.ismartcoding.plain.lib.kgraphql.schema.execution.ExecutionPlan
import com.ismartcoding.plain.lib.kgraphql.schema.execution.TypeCondition
import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.DefinitionNode.*
import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.DefinitionNode.ExecutableDefinitionNode.FragmentDefinitionNode
import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.DefinitionNode.ExecutableDefinitionNode.OperationDefinitionNode
import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.SelectionNode.FieldNode
import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.SelectionNode.FragmentNode
import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.SelectionNode.FragmentNode.FragmentSpreadNode
import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.SelectionNode.FragmentNode.InlineFragmentNode
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.schema.execution.ExecutionOptions
import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.*
import com.ismartcoding.plain.lib.kgraphql.schema.DefaultSchema
import com.ismartcoding.plain.lib.kgraphql.schema.introspection.__Type
import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.DefinitionNode
import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.DirectiveNode
import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.DocumentNode
import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.NameNode
import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.OperationTypeNode
import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.SelectionNode
import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.SelectionSetNode
import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.VariableDefinitionNode
import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.toArguments
import java.util.*
import kotlin.reflect.full.starProjectedType


class RequestInterpreter(val schemaModel: SchemaModel) {

    private val directivesByName = schemaModel.directives.associateBy { it.name }

    inner class InterpreterContext(
        val fragments: Map<String, Pair<Type, SelectionSetNode>>
    ) {
        // prevent stack overflow
        private val fragmentsStack = Stack<String>()
        fun get(node: SelectionNode.FragmentNode.FragmentSpreadNode): Execution.Fragment? {
            if (fragmentsStack.contains(node.name.value)) throw GraphQLError(
                "Fragment spread circular references are not allowed",
                node
            )

            val (conditionType, selectionSet) = fragments[node.name.value] ?: return null
            val condition = TypeCondition(conditionType)

            fragmentsStack.push(node.name.value)
            val elements = selectionSet.selections.map { conditionType.handleSelectionFieldOrFragment(it, this) }
            fragmentsStack.pop()

            return Execution.Fragment(node, condition, elements, node.directives?.lookup())
        }
    }

    fun createExecutionPlan(
        document: DocumentNode,
        requestedOperationName: String?,
        variables: VariablesJson,
        options: ExecutionOptions
    ): ExecutionPlan {
        val test = document.definitions.filterIsInstance<DefinitionNode.ExecutableDefinitionNode>()

        val operation = test.filterIsInstance<DefinitionNode.ExecutableDefinitionNode.OperationDefinitionNode>().let { operations ->
            when (operations.size) {
                0 -> throw GraphQLError("Must provide any operation")
                1 -> operations.first()
                else -> {
                    val operationNamesFound = operations.mapNotNull { it.name?.value }.also {
                        if (it.size != operations.size) throw GraphQLError("anonymous operation must be the only defined operation")
                    }.joinToString(prefix = "[", postfix = "]")

                    val operationName = requestedOperationName ?: (
                        variables.get(String::class, String::class.starProjectedType, DefaultSchema.Companion.OPERATION_NAME_PARAM)
                            ?: throw GraphQLError("Must provide an operation name from: $operationNamesFound")
                        )

                    operations.firstOrNull { it.name?.value == operationName }
                        ?: throw GraphQLError("Must provide an operation name from: $operationNamesFound, found $operationName")
                }
            }
        }

        val root = when (operation.operation) {
            OperationTypeNode.QUERY -> schemaModel.query
            OperationTypeNode.MUTATION -> schemaModel.mutation
                ?: throw GraphQLError("Mutations are not supported on this schema")

            OperationTypeNode.SUBSCRIPTION -> schemaModel.subscription
                ?: throw GraphQLError("Subscriptions are not supported on this schema")
        }

        val fragmentDefinitionNode = test.filterIsInstance<DefinitionNode.ExecutableDefinitionNode.FragmentDefinitionNode>()
        val fragmentDefinitions = fragmentDefinitionNode.map { fragmentDef ->
            val type = schemaModel.allTypesByName.getValue(fragmentDef.typeCondition.name.value)
            val name = fragmentDef.name!!.value

            if (fragmentDefinitionNode.count { it.name!!.value == name } > 1) {
                throw GraphQLError("There can be only one fragment named $name.", fragmentDef)
            }

            name to (type to fragmentDef.selectionSet)
        }.toMap()

        val ctx = InterpreterContext(fragmentDefinitions)

        return ExecutionPlan(
            options,
            operation.selectionSet.selections.map {
                root.handleSelection(it as SelectionNode.FieldNode, ctx, operation.variableDefinitions)
            }
        ).also {
            it.isSubscription = operation.operation == OperationTypeNode.SUBSCRIPTION
        }
    }

    private fun handleReturnType(ctx: InterpreterContext, type: Type, requestNode: SelectionNode.FieldNode) =
        handleReturnType(ctx, type, requestNode.selectionSet, requestNode.name)

    private fun handleReturnType(
        ctx: InterpreterContext,
        type: Type,
        selectionSet: SelectionSetNode?,
        propertyName: NameNode? = null
    ): List<Execution> {
        val children = mutableListOf<Execution>()

        if (!selectionSet?.selections.isNullOrEmpty()) {
            selectionSet!!.selections.mapTo(children) {
                handleReturnTypeChildOrFragment(it, type, ctx)
            }
        } else if (type.unwrapped().fields?.isNotEmpty() == true) {
            throw GraphQLError(
                "Missing selection set on property ${propertyName?.value} of type ${type.unwrapped().name}",
                selectionSet
            )
        }

        return children
    }

    private fun handleReturnTypeChildOrFragment(node: SelectionNode, returnType: Type, ctx: InterpreterContext) =
        returnType.unwrapped().handleSelectionFieldOrFragment(node, ctx)

    private fun findFragmentType(
        fragment: SelectionNode.FragmentNode,
        ctx: InterpreterContext,
        enclosingType: Type
    ): Execution.Fragment = when (fragment) {
        is SelectionNode.FragmentNode.FragmentSpreadNode -> {
            ctx.get(fragment) ?: throw throwUnknownFragmentTypeEx(fragment)
        }

        is SelectionNode.FragmentNode.InlineFragmentNode -> {
            val type = if (fragment.directives?.isNotEmpty() == true) {
                enclosingType
            } else {
                schemaModel.queryTypesByName[fragment.typeCondition?.name?.value] ?: throw throwUnknownFragmentTypeEx(
                    fragment
                )
            }
            Execution.Fragment(
                selectionNode = fragment,
                condition = TypeCondition(type),
                directives = fragment.directives?.lookup(),
                elements = fragment.selectionSet.selections.map { type.handleSelectionFieldOrFragment(it, ctx) }
            )
        }
    }

    private fun Type.handleSelectionFieldOrFragment(node: SelectionNode, ctx: InterpreterContext): Execution =
        when (node) {
            is SelectionNode.FragmentNode -> findFragmentType(node, ctx, this)
            is SelectionNode.FieldNode -> handleSelection(node, ctx)
        }

    private fun Type.handleSelection(
        node: SelectionNode.FieldNode,
        ctx: InterpreterContext,
        variables: List<VariableDefinitionNode>? = null
    ): Execution.Node {
        return when (val field = this[node.name.value]) {
            null -> throw GraphQLError(
                "Property ${node.name.value} on ${name} does not exist",
                node
            )

            is Field.Union<*> -> handleUnion(field, node, ctx)
            else -> {
                validatePropertyArguments(this, field, node)

                return Execution.Node(
                    selectionNode = node,
                    field = field,
                    children = handleReturnType(ctx, field.returnType, node),
                    key = node.name.value,
                    alias = node.alias?.value,
                    arguments = node.arguments?.toArguments(),
                    directives = node.directives?.lookup(),
                    variables = variables
                )
            }
        }
    }

    private fun <T> handleUnion(
        field: Field.Union<T>,
        selectionNode: SelectionNode.FieldNode,
        ctx: InterpreterContext
    ): Execution.Union {
        validateUnionRequest(field, selectionNode)

        val unionMembersChildren: Map<Type, List<Execution>> =
            field.returnType.possibleTypes.associateWith { possibleType ->
                val selections = selectionNode.selectionSet?.selections

                val a = selections?.filterIsInstance<SelectionNode.FragmentNode.FragmentSpreadNode>()?.firstOrNull {
                    ctx.fragments[it.name.value]?.first?.name == possibleType.name
                }

                if (a != null) return@associateWith handleReturnType(
                    ctx,
                    possibleType,
                    ctx.fragments.getValue(a.name.value).second
                )

                val b = selections?.filterIsInstance<SelectionNode.FragmentNode.InlineFragmentNode>()?.find {
                    possibleType.name == it.typeCondition?.name?.value
                }

                if (b != null) return@associateWith handleReturnType(ctx, possibleType, b.selectionSet)

                throw GraphQLError(
                    "Missing type argument for type ${possibleType.name}",
                    selectionNode
                )
            }

        return Execution.Union(
            node = selectionNode,
            unionField = field,
            memberChildren = unionMembersChildren,
            key = selectionNode.name.value,
            alias = selectionNode.alias?.value,
            condition = null,
            directives = selectionNode.directives?.lookup()
        )

    }

    private fun throwUnknownFragmentTypeEx(fragment: SelectionNode.FragmentNode) = when (fragment) {
        is SelectionNode.FragmentNode.FragmentSpreadNode -> throw IllegalArgumentException("This should never happen")
        is SelectionNode.FragmentNode.InlineFragmentNode -> GraphQLError(
            message = "Unknown type ${fragment.typeCondition?.name?.value} in type condition on fragment ${fragment.typeCondition?.name?.value}",
            node = fragment
        )
    }

    fun List<DirectiveNode>.lookup() = associate { findDirective(it) to it.arguments?.toArguments() }

    private fun findDirective(invocation: DirectiveNode): Directive {
        return directivesByName[invocation.name.value.removePrefix("@")]
            ?: throw GraphQLError("Directive ${invocation.name.value} does not exist", invocation)
    }

}
