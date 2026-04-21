package com.ismartcoding.lib.kgraphql.request

import com.ismartcoding.lib.kgraphql.*
import com.ismartcoding.lib.kgraphql.schema.model.ast.TypeNode
import com.ismartcoding.lib.kgraphql.schema.model.ast.ValueNode
import com.ismartcoding.lib.kgraphql.schema.model.ast.VariableDefinitionNode
import com.ismartcoding.lib.kgraphql.GraphQLError
import com.ismartcoding.lib.kgraphql.schema.introspection.TypeKind
import com.ismartcoding.lib.kgraphql.schema.structure.LookupSchema
import com.ismartcoding.lib.kgraphql.schema.structure.Type
import kotlin.reflect.KClass
import kotlin.reflect.KType

@Suppress("UNCHECKED_CAST")
data class Variables(
        private val typeDefinitionProvider: LookupSchema,
        private val variablesJson: VariablesJson,
        private val variables: List<VariableDefinitionNode>?
) {

    /**
     * map and return object of requested class
     */
    fun <T : Any> get(kClass: KClass<T>, kType: KType, typeName: String?, keyNode: ValueNode.VariableNode, transform: (value: ValueNode) -> Any?): T? {
        val variable = variables?.find { keyNode.name.value == it.variable.name.value }
                ?: throw IllegalArgumentException("Variable '$${keyNode.name.value}' was not declared for this operation")

        val isIterable = kClass.isIterable()

        validateVariable(typeDefinitionProvider.typeReference(kType), typeName, variable)

        var value = variablesJson.get(kClass, kType, keyNode.name)
        if(value == null && variable.defaultValue != null){
            value = transformDefaultValue(transform, variable.defaultValue, kClass)
        }

        value?.let {
            if (isIterable && kType.getIterableElementType()?.isMarkedNullable == false) {
                for (element in value as Iterable<*>) {
                    if (element == null) {
                        throw GraphQLError(
                            "Invalid argument value $value from variable $${keyNode.name.value}, expected list with non null arguments",
                            keyNode
                        )
                    }
                }
            }
        }

        return value
    }

    private fun <T : Any> transformDefaultValue(transform: (value: ValueNode) -> Any?, defaultValue: ValueNode, kClass: KClass<T>): T? {
        val transformedDefaultValue = transform.invoke(defaultValue)
        return when {
            transformedDefaultValue == null -> null
            kClass.isInstance(transformedDefaultValue) -> transformedDefaultValue as T?
            else -> throw ExecutionException("Invalid transform function returned")
        }
    }

    private fun validateVariable(expectedType: TypeReference, expectedTypeName: String?, variable: VariableDefinitionNode){
        val variableType = variable.type

        val invalidName = (expectedTypeName ?: expectedType.name) != variable.type.nameNode.value
        val invalidIsList = expectedType.isList != variableType.isList
        val invalidNullability = !expectedType.isNullable && variableType !is TypeNode.NonNullTypeNode && variable.defaultValue == null

        val invalidElementNullability = !expectedType.isElementNullable && when (variableType) {
            is TypeNode.ListTypeNode -> variableType.isElementNullable
            else -> false
        }

        if(invalidName || invalidIsList || invalidNullability || invalidElementNullability){
            throw GraphQLError(
                "Invalid variable $${variable.variable.name.value} argument type ${variableType.nameNode.value}, expected $expectedType",
                variable
            )
        }
    }
}
