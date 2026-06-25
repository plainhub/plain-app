package com.ismartcoding.plain.lib.kgraphql.schema

interface Subscription {
    fun request(n: Long)
    fun cancel()
}