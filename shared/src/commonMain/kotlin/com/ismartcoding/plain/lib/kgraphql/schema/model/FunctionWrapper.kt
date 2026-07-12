@file:Suppress("UNCHECKED_CAST")

package com.ismartcoding.plain.lib.kgraphql.schema.model

import com.ismartcoding.plain.lib.kgraphql.schema.Publisher
import com.ismartcoding.plain.lib.kgraphql.schema.Subscriber
import com.ismartcoding.plain.lib.kgraphql.schema.structure.validateName
import kotlin.reflect.KType
import kotlin.reflect.typeOf

private val subscribers = mutableMapOf<String, Subscriber?>()

/**
 * FunctionWrapper stores functions registered in schema by server code.
 * Up to 9 arguments are supported. Functions are invoked DIRECTLY (no reflection).
 * Metadata (arg names, types, return type) is provided explicitly via reified type parameters
 * and explicit arg name parameters, making this pure Kotlin with no kotlin.reflect dependency.
 */
interface FunctionWrapper<out T> : Publisher {

    /**
     * The Kotlin return type of the wrapped function. Named kReturnType to avoid
     * conflict with Field.returnType (which returns a kgraphql Type, not a KType)
     * in classes that both extend Field and delegate to FunctionWrapper.
     */
    val kReturnType: KType
    val hasReceiver: Boolean
    val argumentsDescriptor: Map<String, KType>

    suspend fun invoke(vararg args: Any?): T?
    suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>): T?

    fun arity(): Int

    fun hasReturnType(): Boolean = kReturnType.classifier != Unit::class

    abstract class Base<T>(
        kReturnType: KType,
        override val hasReceiver: Boolean,
        argDescriptors: List<Pair<String, KType>>
    ) : FunctionWrapper<T> {

        override val kReturnType: KType = kReturnType
        override val argumentsDescriptor: Map<String, KType> = linkedMapOf<String, KType>().apply {
            argDescriptors.forEach { (name, type) ->
                validateName(name)
                put(name, type)
            }
        }
    }

    companion object {
        // ===== ArityZero (hasReceiver=false, 0 GraphQL args) =====
        inline fun <reified T> on(noinline function: suspend () -> T): FunctionWrapper<T> =
            ArityZero(function, typeOf<T>())

        // ===== ArityOne =====
        // hasReceiver=false (1 GraphQL arg)
        inline fun <reified T, reified R> on(argName: String, noinline function: suspend (R) -> T): FunctionWrapper<T> =
            ArityOne(function, false, listOf(argName to typeOf<R>()), typeOf<T>())

        // hasReceiver=true (0 GraphQL args, R is receiver; R not reified — never used in typeOf)
        inline fun <reified T, R> on(noinline function: suspend (R) -> T): FunctionWrapper<T> =
            ArityOne(function, true, emptyList(), typeOf<T>())

        // ===== ArityTwo =====
        // hasReceiver=false (2 GraphQL args)
        inline fun <reified T, reified R, reified E> on(argName1: String, argName2: String, noinline function: suspend (R, E) -> T): FunctionWrapper<T> =
            ArityTwo(function, false, listOf(argName1 to typeOf<R>(), argName2 to typeOf<E>()), typeOf<T>())

        // hasReceiver=true (1 GraphQL arg, R is receiver; R not reified)
        inline fun <reified T, R, reified E> on(argName: String, noinline function: suspend (R, E) -> T): FunctionWrapper<T> =
            ArityTwo(function, true, listOf(argName to typeOf<E>()), typeOf<T>())

        // ===== ArityThree =====
        // hasReceiver=false (3 GraphQL args)
        inline fun <reified T, reified R, reified E, reified W> on(argName1: String, argName2: String, argName3: String, noinline function: suspend (R, E, W) -> T): FunctionWrapper<T> =
            ArityThree(function, false, listOf(argName1 to typeOf<R>(), argName2 to typeOf<E>(), argName3 to typeOf<W>()), typeOf<T>())

        // hasReceiver=true (2 GraphQL args, R is receiver; R not reified)
        inline fun <reified T, R, reified E, reified W> on(argName1: String, argName2: String, noinline function: suspend (R, E, W) -> T): FunctionWrapper<T> =
            ArityThree(function, true, listOf(argName1 to typeOf<E>(), argName2 to typeOf<W>()), typeOf<T>())

        // ===== ArityFour =====
        // hasReceiver=false (4 GraphQL args)
        inline fun <reified T, reified R, reified E, reified W, reified Q> on(argName1: String, argName2: String, argName3: String, argName4: String, noinline function: suspend (R, E, W, Q) -> T): FunctionWrapper<T> =
            ArityFour(function, false, listOf(argName1 to typeOf<R>(), argName2 to typeOf<E>(), argName3 to typeOf<W>(), argName4 to typeOf<Q>()), typeOf<T>())

        // hasReceiver=true (3 GraphQL args, R is receiver; R not reified)
        inline fun <reified T, R, reified E, reified W, reified Q> on(argName1: String, argName2: String, argName3: String, noinline function: suspend (R, E, W, Q) -> T): FunctionWrapper<T> =
            ArityFour(function, true, listOf(argName1 to typeOf<E>(), argName2 to typeOf<W>(), argName3 to typeOf<Q>()), typeOf<T>())

        // ===== ArityFive =====
        // hasReceiver=false (5 GraphQL args)
        inline fun <reified T, reified R, reified E, reified W, reified Q, reified A> on(argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, noinline function: suspend (R, E, W, Q, A) -> T): FunctionWrapper<T> =
            ArityFive(function, false, listOf(argName1 to typeOf<R>(), argName2 to typeOf<E>(), argName3 to typeOf<W>(), argName4 to typeOf<Q>(), argName5 to typeOf<A>()), typeOf<T>())

        // hasReceiver=true (4 GraphQL args, R is receiver; R not reified)
        inline fun <reified T, R, reified E, reified W, reified Q, reified A> on(argName1: String, argName2: String, argName3: String, argName4: String, noinline function: suspend (R, E, W, Q, A) -> T): FunctionWrapper<T> =
            ArityFive(function, true, listOf(argName1 to typeOf<E>(), argName2 to typeOf<W>(), argName3 to typeOf<Q>(), argName4 to typeOf<A>()), typeOf<T>())

        // ===== AritySix =====
        // hasReceiver=false (6 GraphQL args)
        inline fun <reified T, reified R, reified E, reified W, reified Q, reified A, reified S> on(argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, argName6: String, noinline function: suspend (R, E, W, Q, A, S) -> T): FunctionWrapper<T> =
            AritySix(function, false, listOf(argName1 to typeOf<R>(), argName2 to typeOf<E>(), argName3 to typeOf<W>(), argName4 to typeOf<Q>(), argName5 to typeOf<A>(), argName6 to typeOf<S>()), typeOf<T>())

        // hasReceiver=true (5 GraphQL args, R is receiver; R not reified)
        inline fun <reified T, R, reified E, reified W, reified Q, reified A, reified S> on(argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, noinline function: suspend (R, E, W, Q, A, S) -> T): FunctionWrapper<T> =
            AritySix(function, true, listOf(argName1 to typeOf<E>(), argName2 to typeOf<W>(), argName3 to typeOf<Q>(), argName4 to typeOf<A>(), argName5 to typeOf<S>()), typeOf<T>())

        // ===== AritySeven =====
        // hasReceiver=false (7 GraphQL args)
        inline fun <reified T, reified R, reified E, reified W, reified Q, reified A, reified S, reified D> on(argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, argName6: String, argName7: String, noinline function: suspend (R, E, W, Q, A, S, D) -> T): FunctionWrapper<T> =
            AritySeven(function, false, listOf(argName1 to typeOf<R>(), argName2 to typeOf<E>(), argName3 to typeOf<W>(), argName4 to typeOf<Q>(), argName5 to typeOf<A>(), argName6 to typeOf<S>(), argName7 to typeOf<D>()), typeOf<T>())

        // hasReceiver=true (6 GraphQL args, R is receiver; R not reified)
        inline fun <reified T, R, reified E, reified W, reified Q, reified A, reified S, reified D> on(argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, argName6: String, noinline function: suspend (R, E, W, Q, A, S, D) -> T): FunctionWrapper<T> =
            AritySeven(function, true, listOf(argName1 to typeOf<E>(), argName2 to typeOf<W>(), argName3 to typeOf<Q>(), argName4 to typeOf<A>(), argName5 to typeOf<S>(), argName6 to typeOf<D>()), typeOf<T>())

        // ===== ArityEight =====
        // hasReceiver=false (8 GraphQL args)
        inline fun <reified T, reified R, reified E, reified W, reified Q, reified A, reified S, reified D, reified F> on(argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, argName6: String, argName7: String, argName8: String, noinline function: suspend (R, E, W, Q, A, S, D, F) -> T): FunctionWrapper<T> =
            ArityEight(function, false, listOf(argName1 to typeOf<R>(), argName2 to typeOf<E>(), argName3 to typeOf<W>(), argName4 to typeOf<Q>(), argName5 to typeOf<A>(), argName6 to typeOf<S>(), argName7 to typeOf<D>(), argName8 to typeOf<F>()), typeOf<T>())

        // hasReceiver=true (7 GraphQL args, R is receiver; R not reified)
        inline fun <reified T, R, reified E, reified W, reified Q, reified A, reified S, reified D, reified F> on(argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, argName6: String, argName7: String, noinline function: suspend (R, E, W, Q, A, S, D, F) -> T): FunctionWrapper<T> =
            ArityEight(function, true, listOf(argName1 to typeOf<E>(), argName2 to typeOf<W>(), argName3 to typeOf<Q>(), argName4 to typeOf<A>(), argName5 to typeOf<S>(), argName6 to typeOf<D>(), argName7 to typeOf<F>()), typeOf<T>())

        // ===== ArityNine =====
        // hasReceiver=false (9 GraphQL args)
        inline fun <reified T, reified R, reified E, reified W, reified Q, reified A, reified S, reified D, reified F, reified G> on(argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, argName6: String, argName7: String, argName8: String, argName9: String, noinline function: suspend (R, E, W, Q, A, S, D, F, G) -> T): FunctionWrapper<T> =
            ArityNine(function, false, listOf(argName1 to typeOf<R>(), argName2 to typeOf<E>(), argName3 to typeOf<W>(), argName4 to typeOf<Q>(), argName5 to typeOf<A>(), argName6 to typeOf<S>(), argName7 to typeOf<D>(), argName8 to typeOf<F>(), argName9 to typeOf<G>()), typeOf<T>())

        // hasReceiver=true (8 GraphQL args, R is receiver; R not reified)
        inline fun <reified T, R, reified E, reified W, reified Q, reified A, reified S, reified D, reified F, reified G> on(argName1: String, argName2: String, argName3: String, argName4: String, argName5: String, argName6: String, argName7: String, argName8: String, noinline function: suspend (R, E, W, Q, A, S, D, F, G) -> T): FunctionWrapper<T> =
            ArityNine(function, true, listOf(argName1 to typeOf<E>(), argName2 to typeOf<W>(), argName3 to typeOf<Q>(), argName4 to typeOf<A>(), argName5 to typeOf<S>(), argName6 to typeOf<D>(), argName7 to typeOf<F>(), argName8 to typeOf<G>()), typeOf<T>())
    }

    class ArityZero<T>(
        val implementation: suspend () -> T,
        returnType: KType
    ) : Base<T>(returnType, false, emptyList()) {
        override fun arity(): Int = 0

        override suspend fun invoke(vararg args: Any?): T? {
            if (args.isNotEmpty()) {
                val e = IllegalArgumentException("This function does not accept arguments")
                subscribers.forEach { it.value?.onError(e) }
                throw e
            }
            val t = implementation()
            subscribers.forEach { it.value?.onNext(t) }
            return t
        }

        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>): T? {
            TODO("not needed")
        }

        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
    }

    class ArityOne<T, R>(
        val implementation: suspend (R) -> T,
        hasReceiver: Boolean,
        argDescriptors: List<Pair<String, KType>>,
        returnType: KType
    ) : Base<T>(returnType, hasReceiver, argDescriptors) {
        override fun arity(): Int = 1

        override suspend fun invoke(vararg args: Any?): T? {
            if (args.size == arity()) {
                val t = implementation(args[0] as R)
                subscribers.forEach { it.value?.onNext(t) }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach { it.value?.onError(e) }
                throw e
            }
        }

        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>): T? {
            if (args.size == arity()) {
                val t = implementation(args[0] as R)
                val subscription = args[0] as String
                subscribers[subscription]?.setArgs(subscriptionArgs.toTypedArray())
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach { it.value?.onError(e) }
                throw e
            }
        }

        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
    }

    class ArityTwo<T, R, E>(
        val implementation: suspend (R, E) -> T,
        hasReceiver: Boolean,
        argDescriptors: List<Pair<String, KType>>,
        returnType: KType
    ) : Base<T>(returnType, hasReceiver, argDescriptors) {
        override fun arity(): Int = 2

        override suspend fun invoke(vararg args: Any?): T? {
            if (args.size == arity()) {
                val t = implementation(args[0] as R, args[1] as E)
                subscribers.forEach { it.value?.onNext(t) }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach { it.value?.onError(e) }
                throw e
            }
        }

        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>): T? {
            TODO("not needed")
        }

        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
    }

    class ArityThree<T, R, E, W>(
        val implementation: suspend (R, E, W) -> T,
        hasReceiver: Boolean,
        argDescriptors: List<Pair<String, KType>>,
        returnType: KType
    ) : Base<T>(returnType, hasReceiver, argDescriptors) {
        override fun arity(): Int = 3

        override suspend fun invoke(vararg args: Any?): T? {
            if (args.size == arity()) {
                val t = implementation(args[0] as R, args[1] as E, args[2] as W)
                subscribers.forEach { it.value?.onNext(t) }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach { it.value?.onError(e) }
                throw e
            }
        }

        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>): T? {
            TODO("not needed")
        }

        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
    }

    class ArityFour<T, R, E, W, Q>(
        val implementation: suspend (R, E, W, Q) -> T,
        hasReceiver: Boolean,
        argDescriptors: List<Pair<String, KType>>,
        returnType: KType
    ) : Base<T>(returnType, hasReceiver, argDescriptors) {
        override fun arity(): Int = 4

        override suspend fun invoke(vararg args: Any?): T? {
            if (args.size == arity()) {
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q)
                subscribers.forEach { it.value?.onNext(t) }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach { it.value?.onError(e) }
                throw e
            }
        }

        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>): T? {
            TODO("not needed")
        }

        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
    }

    class ArityFive<T, R, E, W, Q, A>(
        val implementation: suspend (R, E, W, Q, A) -> T,
        hasReceiver: Boolean,
        argDescriptors: List<Pair<String, KType>>,
        returnType: KType
    ) : Base<T>(returnType, hasReceiver, argDescriptors) {
        override fun arity(): Int = 5

        override suspend fun invoke(vararg args: Any?): T? {
            if (args.size == arity()) {
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A)
                subscribers.forEach { it.value?.onNext(t) }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach { it.value?.onError(e) }
                throw e
            }
        }

        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>): T? {
            TODO("not needed")
        }

        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
    }

    class AritySix<T, R, E, W, Q, A, S>(
        val implementation: suspend (R, E, W, Q, A, S) -> T,
        hasReceiver: Boolean,
        argDescriptors: List<Pair<String, KType>>,
        returnType: KType
    ) : Base<T>(returnType, hasReceiver, argDescriptors) {
        override fun arity(): Int = 6

        override suspend fun invoke(vararg args: Any?): T? {
            if (args.size == arity()) {
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S)
                subscribers.forEach { it.value?.onNext(t) }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach { it.value?.onError(e) }
                throw e
            }
        }

        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>): T? {
            TODO("not needed")
        }

        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
    }

    class AritySeven<T, R, E, W, Q, A, S, D>(
        val implementation: suspend (R, E, W, Q, A, S, D) -> T,
        hasReceiver: Boolean,
        argDescriptors: List<Pair<String, KType>>,
        returnType: KType
    ) : Base<T>(returnType, hasReceiver, argDescriptors) {
        override fun arity(): Int = 7

        override suspend fun invoke(vararg args: Any?): T? {
            if (args.size == arity()) {
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D)
                subscribers.forEach { it.value?.onNext(t) }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach { it.value?.onError(e) }
                throw e
            }
        }

        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>): T? {
            TODO("not needed")
        }

        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
    }

    class ArityEight<T, R, E, W, Q, A, S, D, F>(
        val implementation: suspend (R, E, W, Q, A, S, D, F) -> T,
        hasReceiver: Boolean,
        argDescriptors: List<Pair<String, KType>>,
        returnType: KType
    ) : Base<T>(returnType, hasReceiver, argDescriptors) {
        override fun arity(): Int = 8

        override suspend fun invoke(vararg args: Any?): T? {
            if (args.size == arity()) {
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F)
                subscribers.forEach { it.value?.onNext(t) }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach { it.value?.onError(e) }
                throw e
            }
        }

        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>): T? {
            TODO("not needed")
        }

        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
    }

    class ArityNine<T, R, E, W, Q, A, S, D, F, G>(
        val implementation: suspend (R, E, W, Q, A, S, D, F, G) -> T,
        hasReceiver: Boolean,
        argDescriptors: List<Pair<String, KType>>,
        returnType: KType
    ) : Base<T>(returnType, hasReceiver, argDescriptors) {
        override fun arity(): Int = 9

        override suspend fun invoke(vararg args: Any?): T? {
            if (args.size == arity()) {
                val t = implementation(args[0] as R, args[1] as E, args[2] as W, args[3] as Q, args[4] as A, args[5] as S, args[6] as D, args[7] as F, args[8] as G)
                subscribers.forEach { it.value?.onNext(t) }
                return t
            } else {
                val e = IllegalArgumentException("This function needs exactly ${arity()} arguments")
                subscribers.forEach { it.value?.onError(e) }
                throw e
            }
        }

        override suspend fun invoke(args: List<Any?>, subscriptionArgs: List<String>): T? {
            TODO("not needed")
        }

        override fun subscribe(subscription: String, subscriber: Subscriber) {
            subscribers[subscription] = subscriber
        }

        override fun unsubscribe(subscription: String) {
            subscribers.remove(subscription)
        }
    }
}
