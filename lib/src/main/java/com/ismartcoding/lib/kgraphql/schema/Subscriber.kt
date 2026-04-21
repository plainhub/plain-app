package com.ismartcoding.lib.kgraphql.schema

import com.fasterxml.jackson.databind.ObjectWriter

interface Subscriber {
    fun onSubscribe(subscription: Subscription)

    fun onNext(item: Any?)

    fun setArgs(args: Array<String>)

    fun onError(throwable: Throwable)

    fun onComplete()

    fun setObjectWriter(objectWriter: ObjectWriter)
}