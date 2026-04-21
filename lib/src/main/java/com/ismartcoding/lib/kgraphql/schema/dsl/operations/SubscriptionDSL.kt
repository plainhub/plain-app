package com.ismartcoding.lib.kgraphql.schema.dsl.operations

import com.ismartcoding.lib.kgraphql.schema.Publisher
import com.ismartcoding.lib.kgraphql.schema.SchemaException
import com.ismartcoding.lib.kgraphql.schema.Subscriber
import com.ismartcoding.lib.kgraphql.schema.Subscription
import com.ismartcoding.lib.kgraphql.schema.model.FunctionWrapper
import com.ismartcoding.lib.kgraphql.schema.model.SubscriptionDef
import com.fasterxml.jackson.databind.ObjectWriter
import kotlin.reflect.full.isSubtypeOf
import kotlin.reflect.full.memberProperties
import kotlin.reflect.full.starProjectedType


class SubscriptionDSL(
    name: String
) : AbstractOperationDSL(name) {

    internal fun toKQLSubscription(): SubscriptionDef<out Any?> {
        val function =
            functionWrapper ?: throw IllegalArgumentException("resolver has to be specified for query [$name]")

        return SubscriptionDef(
            name = name,
            resolver = function,
            description = description,
            isDeprecated = isDeprecated,
            deprecationReason = deprecationReason,
            inputValues = inputValues,
            accessRule = accessRuleBlock
        )
    }
}



private fun <T : Any> getFieldValue(clazz: T, field: String): Any? {
    val properties = clazz.javaClass.kotlin.memberProperties
    for (p in properties) {
        if (p.name == field) {
            return p.getter.call(clazz)
        }
    }
    return null
}

fun <T : Any> subscribe(subscription: String, publisher: Publisher, output: T, function: (response: String) -> Unit): T {
    if (!(publisher as FunctionWrapper<*>).kFunction.returnType.isSubtypeOf(output::class.starProjectedType))  
        throw SchemaException("Subscription return type must be the same as the publisher's")
    val subscriber = object : Subscriber {
        override fun setObjectWriter(objectWriter: ObjectWriter) {
            this.objectWriter = objectWriter
        }

        private var args = emptyArray<String>()
        private lateinit var objectWriter: ObjectWriter
        override fun setArgs(args: Array<String>) {
            this.args = args
        }

        override fun onNext(item: Any?) {
            val response = mutableMapOf<String, Any?>()
            val result = mutableMapOf<String, Any?>()
            response["data"] = result
            args.forEach {
                result[it] = getFieldValue(item!!, it)
            }
            function(objectWriter.writeValueAsString(response))
        }

        override fun onComplete() {
            TODO("not needed for now")
        }

        override fun onSubscribe(subscription: Subscription) {
            TODO("not needed for now")
        }

        override fun onError(throwable: Throwable) {
            publisher.unsubscribe(subscription)
        }

    }
    publisher.subscribe(subscription, subscriber)
    return output
}

fun <T : Any> unsubscribe(subscription: String, publisher: Publisher, output: T): T {
    publisher.unsubscribe(subscription)
    return output
}