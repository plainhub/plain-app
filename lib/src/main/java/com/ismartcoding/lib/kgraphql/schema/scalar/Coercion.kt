package com.ismartcoding.lib.kgraphql.schema.scalar

import com.ismartcoding.lib.kgraphql.ExecutionException
import com.ismartcoding.lib.kgraphql.dropQuotes
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.ismartcoding.lib.kgraphql.schema.execution.Execution
import com.ismartcoding.lib.kgraphql.schema.model.ast.ValueNode
import com.ismartcoding.lib.kgraphql.schema.model.ast.ValueNode.*
import com.ismartcoding.lib.kgraphql.GraphQLError
import com.ismartcoding.lib.kgraphql.schema.builtin.*
import com.ismartcoding.lib.kgraphql.schema.structure.Type
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonPrimitive

private typealias JsonValueNode = com.fasterxml.jackson.databind.node.ValueNode

@Suppress("UNCHECKED_CAST")
// TODO: Re-structure scalars, as it's a bit too complicated now.
fun <T : Any> deserializeScalar(scalar: Type.Scalar<T>, value : ValueNode): T {
    try {
        return when(scalar.coercion){
            //built in scalars
            STRING_COERCION -> STRING_COERCION.deserialize(value.valueNodeName, value as StringValueNode) as T
            FLOAT_COERCION -> FLOAT_COERCION.deserialize(value.valueNodeName, value) as T
            DOUBLE_COERCION -> DOUBLE_COERCION.deserialize(value.valueNodeName, value) as T
            SHORT_COERCION -> SHORT_COERCION.deserialize(value.valueNodeName, value) as T
            INT_COERCION -> INT_COERCION.deserialize(value.valueNodeName, value) as T
            BOOLEAN_COERCION -> BOOLEAN_COERCION.deserialize(value.valueNodeName, value) as T
            LONG_COERCION -> LONG_COERCION.deserialize(value.valueNodeName, value) as T

            is StringScalarCoercion<T> -> scalar.coercion.deserialize(value.valueNodeName.dropQuotes(), value)
            is ShortScalarCoercion<T> -> scalar.coercion.deserialize(value.valueNodeName.toShort(), value)
            is IntScalarCoercion<T> -> scalar.coercion.deserialize(value.valueNodeName.toInt(), value)
            is DoubleScalarCoercion<T> -> scalar.coercion.deserialize(value.valueNodeName.toDouble(), value)
            is BooleanScalarCoercion<T> -> scalar.coercion.deserialize(value.valueNodeName.toBoolean(), value)
            is LongScalarCoercion<T> -> scalar.coercion.deserialize(value.valueNodeName.toLong(), value)
            else -> throw GraphQLError(
                "Unsupported coercion for scalar type ${scalar.name}",
                value
            )
        }
    } catch (e: Exception) {
        throw if (e is GraphQLError) e
        else GraphQLError(
            message = "argument '${value.valueNodeName}' is not valid value of type ${scalar.name}",
            nodes = listOf(value),
            originalError = e
        )
    }
}

@Suppress("UNCHECKED_CAST")
fun <T> serializeScalar(jsonNodeFactory: JsonNodeFactory, scalar: Type.Scalar<*>, value: T, executionNode: Execution): JsonValueNode = when(scalar.coercion){
    is StringScalarCoercion<*> -> {
        jsonNodeFactory.textNode((scalar.coercion as StringScalarCoercion<T>).serialize(value))
    }
    is ShortScalarCoercion<*> -> {
        jsonNodeFactory.numberNode((scalar.coercion as ShortScalarCoercion<T>).serialize(value))
    }
    is IntScalarCoercion<*> -> {
        jsonNodeFactory.numberNode((scalar.coercion as IntScalarCoercion<T>).serialize(value))
    }
    is DoubleScalarCoercion<*> -> {
        jsonNodeFactory.numberNode((scalar.coercion as DoubleScalarCoercion<T>).serialize(value))
    }
    is LongScalarCoercion<*> -> {
        jsonNodeFactory.numberNode((scalar.coercion as LongScalarCoercion<T>).serialize(value))
    }
    is BooleanScalarCoercion<*> -> {
        jsonNodeFactory.booleanNode((scalar.coercion as BooleanScalarCoercion<T>).serialize(value))
    }
    else -> throw ExecutionException("Unsupported coercion for scalar type ${scalar.name}", executionNode)
}

@Suppress("UNCHECKED_CAST")
fun <T> serializeScalar(scalar: Type.Scalar<*>, value: T, executionNode: Execution): JsonElement = when (scalar.coercion) {
    is StringScalarCoercion<*> -> {
        JsonPrimitive((scalar.coercion as StringScalarCoercion<T>).serialize(value))
    }
    is ShortScalarCoercion<*> -> {
        JsonPrimitive((scalar.coercion as ShortScalarCoercion<T>).serialize(value))
    }
    is IntScalarCoercion<*> -> {
        JsonPrimitive((scalar.coercion as IntScalarCoercion<T>).serialize(value))
    }
    is DoubleScalarCoercion<*> -> {
        JsonPrimitive((scalar.coercion as DoubleScalarCoercion<T>).serialize(value))
    }
    is LongScalarCoercion<*> -> {
        JsonPrimitive((scalar.coercion as LongScalarCoercion<T>).serialize(value))
    }
    is BooleanScalarCoercion<*> -> {
        JsonPrimitive((scalar.coercion as BooleanScalarCoercion<T>).serialize(value))
    }
    else -> throw ExecutionException("Unsupported coercion for scalar type ${scalar.name}", executionNode)
}

