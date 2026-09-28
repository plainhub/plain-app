/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
/**
 * Vendored from io.ktor:ktor-websockets:3.5.2.
 */

@file:JvmMultifileClass
@file:JvmName("UtilsKt")

package com.ismartcoding.plain.lib.ktorserver.websocket

import java.nio.*

internal fun ByteBuffer.xor(other: ByteBuffer) {
    val bb = slice()
    val mask = other.slice()
    val maskSize = mask.remaining()

    for (i in 0 until bb.remaining()) {
        bb.put(i, bb.get(i) xor mask[i % maskSize])
    }
}

internal val OUTGOING_CHANNEL_CAPACITY: Int?
    get() = System.getProperty("com.ismartcoding.plain.lib.ktorserver.websocket.outgoingChannelCapacity")?.toInt()
