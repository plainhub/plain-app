package com.ismartcoding.lib.kgraphql.schema.execution

import com.ismartcoding.lib.kgraphql.ExecutionException
import com.ismartcoding.lib.kgraphql.request.Variables
import com.ismartcoding.lib.kgraphql.schema.DefaultSchema
import com.ismartcoding.lib.kgraphql.schema.model.ast.ValueNode
import com.ismartcoding.lib.kgraphql.GraphQLError
import com.ismartcoding.lib.kgraphql.schema.scalar.deserializeScalar
import com.ismartcoding.lib.kgraphql.schema.structure.InputValue
import com.ismartcoding.lib.kgraphql.schema.structure.Type
import kotlin.reflect.KType
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.jvmErasure


open class ArgumentTransformer(val schema : DefaultSchema) {

    private fun transformValue(type: Type, value: ValueNode, variables: Variables) : Any? {
        val kType = type.toKType()
        val typeName = type.unwrapped().name

        return when {
            value is ValueNode.VariableNode -> {
                variables.get(kType.jvmErasure, kType, typeName, value) { subValue ->
                    transformValue(type, subValue, variables)
                }
            }
            value is ValueNode.ObjectValueNode -> {
                val constructor = type.unwrapped().kClass!!.primaryConstructor ?: throw GraphQLError("Java class '${type.unwrapped().kClass!!.simpleName}' as inputType are not supported", value)
                val params = constructor.parameters.associateBy { it.name }
                val valueMap = value.fields.map { valueField ->
                    val inputField = type
                            .unwrapped()
                            .inputFields
                            ?.firstOrNull { it.name == valueField.name.value }
                            ?: throw GraphQLError("Constructor Parameter '${valueField.name.value}' can not be found in '${type.unwrapped().kClass!!.simpleName}'", value)

                    val paramType = inputField
                            .type as? Type
                        ?: throw GraphQLError("Something went wrong while searching for the constructor parameter type : '${valueField.name.value}'", value)

                    params.getValue(valueField.name.value) to transformValue(paramType, valueField.value, variables)
                }.toMap()

                val missingNonOptionalInputs = params.values
                        .filter { !it.isOptional && !valueMap.containsKey(it) }

                if (missingNonOptionalInputs.isNotEmpty()) {
                    val inputs = missingNonOptionalInputs.map { it.name }.joinToString(",")
                    throw GraphQLError("You are missing non optional input fields: $inputs", value)
                }

                constructor.callBy(valueMap)
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

        val kClass = kType.jvmErasure

        fun throwInvalidEnumValue(enumType : Type.Enum<*>){
            throw GraphQLError(
                "Invalid enum ${schema.model.enums[kClass]?.name} value. Expected one of ${enumType.values}", value
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
        assert(inputValue.type.isList())
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
