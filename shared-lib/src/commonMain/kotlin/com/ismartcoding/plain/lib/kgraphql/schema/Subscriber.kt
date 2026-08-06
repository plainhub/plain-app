package com.ismartcoding.plain.lib.kgraphql.schema

interface Subscriber {
    fun onSubscribe(subscription: Subscription)

    fun onNext(item: Any?)

    fun setArgs(args: Array<String>)

    fun onError(throwable: Throwable)

    fun onComplete()
}