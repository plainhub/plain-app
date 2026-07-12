package com.ismartcoding.plain.lib.kgraphql

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.reflect.KClass
import kotlin.reflect.KType

internal fun <T : Any> KClass<T>.defaultKQLTypeName() = this.simpleName!!

internal fun KType.kClass(): KClass<*> = classifier as? KClass<*>
    ?: throw IllegalStateException("Cannot get KClass from KType $this")

internal fun KType.defaultKQLTypeName() = this.kClass().defaultKQLTypeName()

internal fun String.dropQuotes() : String = if(isLiteral()) drop(1).dropLast(1) else this

internal fun String.isLiteral() : Boolean = startsWith('\"') && endsWith('\"')

private val ITERABLE_QNS = setOf(
    "kotlin.collections.Iterable",
    "kotlin.collections.Collection",
    "kotlin.collections.List",
    "kotlin.collections.Set",
    "kotlin.collections.MutableList",
    "kotlin.collections.MutableSet",
    "kotlin.collections.MutableCollection",
    // Java collection types — needed because typeOf<T>() captures exact declared types
    // (e.g. arrayListOf() returns java.util.ArrayList, not kotlin.collections.List)
    "java.util.ArrayList",
    "java.util.LinkedList",
    "java.util.List",
    "java.util.Collection",
    "java.util.Set",
    "java.util.HashSet",
    "java.util.TreeSet",
    "java.util.LinkedHashSet",
)

internal fun KClass<*>.isIterable() = qualifiedName in ITERABLE_QNS

internal fun KType.isIterable() = kClass().isIterable() || toString().startsWith("kotlin.Array")

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
