package com.ismartcoding.plain.lib.kgraphql

import kotlin.reflect.KClass
import kotlin.reflect.KType
import kotlin.reflect.KTypeProjection
import kotlin.reflect.full.createType

internal actual fun KClass<*>.createKType(args: List<KTypeProjection>, nullable: Boolean): KType =
    createType(args, nullable = nullable)
