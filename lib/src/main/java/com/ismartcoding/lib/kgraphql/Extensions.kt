package com.ismartcoding.lib.kgraphql

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.KParameter
import kotlin.reflect.KType
import kotlin.reflect.full.isSubclassOf
import kotlin.reflect.jvm.jvmErasure

internal fun <T : Any> KClass<T>.defaultKQLTypeName() = this.simpleName!!

internal fun KType.defaultKQLTypeName() = this.jvmErasure.defaultKQLTypeName()

internal fun String.dropQuotes() : String = if(isLiteral()) drop(1).dropLast(1) else this

internal fun String.isLiteral() : Boolean = startsWith('\"') && endsWith('\"')

internal fun KParameter.isNullable() = type.isMarkedNullable

internal fun KParameter.isNotNullable() = !type.isMarkedNullable

internal fun KClass<*>.isIterable() = isSubclassOf(Iterable::class)

internal fun KType.isIterable() = jvmErasure.isIterable() || toString().startsWith("kotlin.Array")

internal fun KType.getIterableElementType(): KType? {
    require(isIterable()) { "KType $this is not collection type" }
    return arguments.firstOrNull()?.type ?: throw NoSuchElementException("KType $this has no type arguments")
}


internal fun not(boolean: Boolean) = !boolean



internal suspend fun <T, R> Collection<T>.toMapAsync(
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
    block: suspend (T) -> R
): Map<T, R> = coroutineScope {
    val channel = Channel<Pair<T, R>>()
    val jobs = map { item ->
        launch(dispatcher) {
            try {
                val res = block(item)
                channel.send(item to res)
            } catch (e: Exception) {
                channel.close(e)
            }
        }
    }
    val resultMap = mutableMapOf<T, R>()
    repeat(size) {
        try {
            val (item, result) = channel.receive()
            resultMap[item] = result
        } catch (e: Exception) {
            jobs.forEach { job: Job -> job.cancel() }
            throw e
        }
    }

    channel.close()
    resultMap
}
