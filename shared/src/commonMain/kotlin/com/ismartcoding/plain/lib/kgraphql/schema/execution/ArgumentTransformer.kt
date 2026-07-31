package com.ismartcoding.plain.lib.kgraphql.schema.execution

import com.ismartcoding.plain.lib.kgraphql.ExecutionException
import com.ismartcoding.plain.lib.kgraphql.generated.GeneratedSchemaRegistry
import com.ismartcoding.plain.lib.kgraphql.request.Variables
import com.ismartcoding.plain.lib.kgraphql.schema.DefaultSchema
import com.ismartcoding.plain.lib.kgraphql.schema.model.ast.ValueNode
import com.ismartcoding.plain.lib.kgraphql.GraphQLError
import com.ismartcoding.plain.lib.kgraphql.schema.scalar.deserializeScalar
import com.ismartcoding.plain.lib.kgraphql.kClass
import com.ismartcoding.plain.lib.kgraphql.schema.structure.InputValue
import com.ismartcoding.plain.lib.kgraphql.schema.structure.Type
import kotlin.collections.get
import kotlin.reflect.KType


open class ArgumentTransformer(val schema : DefaultSchema) {

    private fun transformValue(type: Type, value: ValueNode, variables: Variables) : Any? {
        val kType = type.toKType()
        val typeName = type.unwrapped().name

        return when {
            value is ValueNode.VariableNode -> {
                variables.get(kType.kClass(), kType, typeName, value) { subValue ->
                    transformValue(type, subValue, variables)
                }
            }
            value is ValueNode.ObjectValueNode -> {
                val kClass = type.unwrapped().kClass ?: throw GraphQLError("Cannot get KClass from type", value)

                val valueMap = value.fields.map { valueField ->
                    val inputField = type
                            .unwrapped()
                            .inputFields
                            ?.firstOrNull { it.name == valueField.name.value }
                            ?: throw GraphQLError("Constructor Parameter '${valueField.name.value}' can not be found in '${kClass.simpleName}'", value)

                    val paramType = inputField
                            .type as? Type
                        ?: throw GraphQLError("Something went wrong while searching for the constructor parameter type : '${valueField.name.value}'", value)

                    valueField.name.value to transformValue(paramType, valueField.value, variables)
                }.toMap()

                // KSP-only: use the generated InputDescriptor's fromMap factory
                // (direct constructor call) instead of callConstructorBy reflection.
                val inputDescriptor = GeneratedSchemaRegistry.inputs[kClass]
                    ?: throw GraphQLError("No InputDescriptor found for type ${kClass.simpleName}. " +
                        "Annotate the class with @GraphQLInput to enable KSP code generation.", value)

                val missingNonOptionalInputs = inputDescriptor.fields
                    .map { it.name }
                    .filter { it !in inputDescriptor.optionalParams && it !in valueMap }

                if (missingNonOptionalInputs.isNotEmpty()) {
                    val inputs = missingNonOptionalInputs.joinToString(",")
                    throw GraphQLError("You are missing non optional input fields: $inputs", value)
                }

                @Suppress("UNCHECKED_CAST")
                return (inputDescriptor.fromMap as (Map<String, Any?>) -> Any)(valueMap)
            }
            value is ValueNode.NullValueNode -> {
                if (type.isNotNullable()) {
                    throw GraphQLError(
                        "argument '${value.valueNodeName}' is not valid value of type ${type.unwrapped().name}",
                        value
                    )
                } else null
            }
            value is ValueNode.ListValueNode && type.isList() -> {
                if (type.isNotList()) {
                    throw GraphQLError(
                        "argument '${value.valueNodeName}' is not valid value of type ${type.unwrapped().name}",
                        value
                    )
                } else {
                    value.values.map { valueNode ->
                        transformValue(type.unwrapList(), valueNode, variables)
                    }
                }
            }
            else -> transformString(value, kType)
        }
    }

    private fun transformString(value: ValueNode, kType: KType): Any {

        val kClass = kType.kClass()

        fun throwInvalidEnumValue(enumType : Type.Enum<*>){
            val validValues = enumType.values.joinToString(prefix = "[", postfix = "]") { it.name }
            throw GraphQLError(
                "Invalid enum ${schema.model.enums[kClass]?.name} value. Expected one of $validValues", value
            )
        }

        schema.model.enums[kClass]?.let { enumType ->
            return if (value is ValueNode.EnumValueNode) {
                enumType.values.find { it.name == value.value }?.value ?: throwInvalidEnumValue(enumType)
            } else throw GraphQLError(
                "String literal '${value.valueNodeName}' is invalid value for enum type ${enumType.name}",
                value
            )
        } ?: schema.model.scalars[kClass]?.let { scalarType ->
            return deserializeScalar(scalarType, value)
        } ?: throw GraphQLError(
            "Invalid argument value '${value.valueNodeName}' for type ${schema.model.inputTypes[kClass]?.name}",
            value
        )
    }

    fun transformCollectionElementValue(inputValue: InputValue<*>, value: ValueNode, variables: Variables): Any? {
        if (!inputValue.type.isList()) {
            throw ExecutionException("Input value '${inputValue.name}' is not a list type", value)
        }
        val elementType = inputValue.type.unwrapList().ofType as Type?
            ?: throw ExecutionException("Unable to handle value of element of collection without type", value)

        return transformValue(elementType, value, variables)
    }

    fun transformPropertyValue(parameter: InputValue<*>, value: ValueNode, variables: Variables): Any? {
        return transformValue(parameter.type, value, variables)
    }

    fun transformPropertyObjectValue(parameter: InputValue<*>, value: ValueNode.ObjectValueNode, variables: Variables): Any? {
        return transformValue(parameter.type, value, variables)
    }
}
