package com.ismartcoding.lib.kgraphql.schema.dsl.types

import com.ismartcoding.lib.kgraphql.schema.SchemaException
import com.ismartcoding.lib.kgraphql.schema.model.ast.ValueNode
import com.ismartcoding.lib.kgraphql.schema.scalar.DoubleScalarCoercion
import com.ismartcoding.lib.kgraphql.schema.scalar.ScalarCoercion
import kotlin.reflect.KClass


class DoubleScalarDSL<T : Any>(kClass: KClass<T>) : ScalarDSL<T, Double>(kClass) {

    override fun createCoercionFromFunctions(): ScalarCoercion<T, Double> {
        return object : DoubleScalarCoercion<T> {

            val serializeImpl = serialize ?: throw SchemaException(PLEASE_SPECIFY_COERCION)

            val deserializeImpl = deserialize ?: throw SchemaException(PLEASE_SPECIFY_COERCION)

            override fun serialize(instance: T): Double = serializeImpl(instance)

            override fun deserialize(raw: Double, valueNode: ValueNode?): T = deserializeImpl(raw)
        }
    }

}
