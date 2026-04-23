package com.ismartcoding.lib.kgraphql.schema.dsl.types


import com.ismartcoding.lib.kgraphql.schema.SchemaException
import com.ismartcoding.lib.kgraphql.schema.model.ast.ValueNode
import com.ismartcoding.lib.kgraphql.schema.scalar.ShortScalarCoercion
import com.ismartcoding.lib.kgraphql.schema.scalar.ScalarCoercion
import kotlin.reflect.KClass


class ShortScalarDSL<T : Any>(kClass: KClass<T>) : ScalarDSL<T, Short>(kClass) {

    override fun createCoercionFromFunctions(): ScalarCoercion<T, Short> {
        return object : ShortScalarCoercion<T> {

            val serializeImpl = serialize ?: throw SchemaException(PLEASE_SPECIFY_COERCION)

            val deserializeImpl = deserialize ?: throw SchemaException(PLEASE_SPECIFY_COERCION)

            override fun serialize(instance: T): Short = serializeImpl(instance)

            override fun deserialize(raw: Short, valueNode: ValueNode?): T = deserializeImpl(raw)
        }
    }

}
