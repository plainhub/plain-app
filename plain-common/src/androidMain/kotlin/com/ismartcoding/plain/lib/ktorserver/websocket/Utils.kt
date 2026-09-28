/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/
/**
 * Vendored from io.ktor:ktor-websockets:3.5.2.
 */

@file:kotlin.jvm.JvmMultifileClass
@file:kotlin.jvm.JvmName("UtilsKt")

package com.ismartcoding.plain.lib.ktorserver.websocket

@Suppress("NOTHING_TO_INLINE")
internal inline infix fun Byte.xor(other: Byte) = toInt().xor(other.toInt()).toByte()

@Suppress("NOTHING_TO_INLINE")
internal inline fun Boolean.flagAt(at: Int) = if (this) 1 shl at else 0
