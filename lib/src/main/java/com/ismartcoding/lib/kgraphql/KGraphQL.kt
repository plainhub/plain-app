package com.ismartcoding.lib.kgraphql

import com.ismartcoding.lib.kgraphql.schema.Schema
import com.ismartcoding.lib.kgraphql.schema.dsl.SchemaBuilder


class KGraphQL {
    companion object {
        fun schema(init: SchemaBuilder.() -> Unit): Schema {
            return SchemaBuilder()
                .apply(init)
                .build()
        }
    }
}