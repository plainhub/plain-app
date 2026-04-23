package com.ismartcoding.lib.kgraphql.schema.scalar

import com.ismartcoding.lib.kgraphql.schema.model.ast.ValueNode

/**
 * Scalar resolves to a single scalar object, and can't have sub-selections in the request.
 * ScalarSupport defines strategy of handling supported scalar type.
 */
interface ScalarCoercion<Scalar, Raw> {

    /**
     * strategy for scalar serialization
     */
    fun serialize(instance: Scalar) : Raw

    /**
     * strategy for scalar deserialization
     */
    fun deserialize(raw: Raw, valueNode: ValueNode? = null): Scalar

}

