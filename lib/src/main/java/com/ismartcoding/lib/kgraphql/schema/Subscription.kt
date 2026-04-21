package com.ismartcoding.lib.kgraphql.schema

interface Subscription {
    fun request(n: Long)
    fun cancel()
}