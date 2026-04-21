package com.ismartcoding.lib.kgraphql.schema.model


data class Deprecation<T> (val target: T, val reason: String?)