/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

/**
 * Vendored from io.ktor:ktor-server-core:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.core.util

import io.ktor.http.*
import com.ismartcoding.plain.lib.ktorserver.core.plugins.*
import io.ktor.util.converters.*
import io.ktor.util.reflect.*
import kotlin.reflect.*

/**
 * Operator function that allows to delegate variables by call parameters.
 * It does conversion to type [R] using [DefaultConversionService]
 * If [R] is nullable and no values are associated with the delegated property name, returns `null`.
 *
 * Example
 *
 * ```
 * get("/{path}") {
 *     val path: Int by call.pathParameters
 *     val query: String? by call.queryParameters
 *     // ...
 * }
 * ```
 *
 * @throws MissingRequestParameterException if no values associated with name and [R] is not nullable
 * @throws ParameterConversionException when conversion from String to [R] fails
 *
 */
public inline operator fun <reified R> Parameters.getValue(thisRef: Any?, property: KProperty<*>): R =
    getOrFail<R>(property.name)

/**
 * Get parameters value associated with this [name] or fail with [MissingRequestParameterException]
 *
 *
 * @throws MissingRequestParameterException if no values associated with this [name]
 */
@Suppress("NOTHING_TO_INLINE")
public inline fun Parameters.getOrFail(name: String): String =
    get(name) ?: throw MissingRequestParameterException(name)

/**
 * Get parameter value associated with this [name] converting to type [R] using [DefaultConversionService]
 * or fail with [MissingRequestParameterException]
 *
 *
 * @throws MissingRequestParameterException if no values associated with this [name]
 * @throws ParameterConversionException when conversion from String to [R] fails
 */
public inline fun <reified R> Parameters.getOrFail(name: String): R =
    getOrFailImpl(name, typeInfo<R>())

@PublishedApi
internal fun <R> Parameters.getOrFailImpl(name: String, typeInfo: TypeInfo): R {
    return if (typeInfo.kotlinType?.isMarkedNullable == true && get(name) == null) {
        null as R
    } else {
        val values = getAll(name) ?: throw MissingRequestParameterException(name)
        try {
            @Suppress("UNCHECKED_CAST")
            DefaultConversionService.fromValues(values, typeInfo) as R
        } catch (cause: Exception) {
            throw ParameterConversionException(name, typeInfo.type.simpleName ?: typeInfo.type.toString(), cause)
        }
    }
}
