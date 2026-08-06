package com.ismartcoding.plain.lib.kgraphql.schema.execution

import com.ismartcoding.plain.lib.kgraphql.Context
import com.ismartcoding.plain.lib.kgraphql.request.Variables
import com.ismartcoding.plain.lib.kgraphql.schema.DefaultSchema
import com.ismartcoding.plain.lib.kgraphql.schema.introspection.TypeKind
import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.ArgumentNodes
import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.ValueNode.*
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.schema.structure.InputValue
import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.ValueNode


internal class ArgumentsHandler(schema : DefaultSchema) : ArgumentTransformer(schema) {

    fun transformArguments (
        funName: String,
        inputValues: List<InputValue<*>>,
        args: ArgumentNodes?,
        variables: Variables,
        executionNode: Execution,
        requestContext: Context
    ) : List<Any?>{
        val unsupportedArguments = args?.filter { arg ->
            inputValues.none { value -> value.name == arg.key }
        }

        if(unsupportedArguments?.isNotEmpty() == true){
            throw GraphQLError(
                "$funName does support arguments ${inputValues.map { it.name }}. found arguments ${args.keys}",
                executionNode.selectionNode
            )
        }

        return inputValues.map { parameter ->
            val value = args?.get(parameter.name)

            when {
                //inject request context
                parameter.type.isInstance(requestContext) -> requestContext
                parameter.type.isInstance(executionNode) -> executionNode
                value == null && parameter.type.kind != TypeKind.NON_NULL -> parameter.default
                value == null && parameter.type.kind == TypeKind.NON_NULL -> {
                    parameter.default ?: throw GraphQLError(
                        "argument '${parameter.name}' of type ${schema.typeReference(parameter.type)} on field '$funName' is not nullable, value cannot be null",
                        executionNode.selectionNode
                    )
                }
                value is ValueNode.ListValueNode && parameter.type.isList() -> {
                    value.values.map { element ->
                        transformCollectionElementValue(parameter, element, variables)
                    }
                }
                value is ValueNode.ObjectValueNode && parameter.type.isNotList() -> {
                    transformPropertyObjectValue(parameter, value, variables)
                }
                else -> {
                    val transformedValue = transformPropertyValue(parameter, value!!, variables)
                    if (transformedValue == null && parameter.type.isNotNullable()) {
                        throw GraphQLError(
                            "argument ${parameter.name} is not optional, value cannot be null",
                            executionNode.selectionNode
                        )
                    }
                    transformedValue
                }
            }
        }
    }
}
