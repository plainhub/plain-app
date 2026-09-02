/*
 * Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
/**
 * Vendored from io.ktor:ktor-websockets:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.websocket.internals

import io.ktor.utils.io.core.*
import kotlinx.io.*

@OptIn(InternalIoApi::class)
internal fun Source.endsWith(data: ByteArray): Boolean {
    buffer.copy().apply {
        discard(remaining - data.size)
        return readByteArray().contentEquals(data)
    }
}
