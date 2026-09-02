/*
* Copyright 2014-2021 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
*/
/**
 * Vendored from io.ktor:ktor-websockets:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.websocket

import io.ktor.utils.io.charsets.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.io.*

/**
 * Reads text content from the text frame.
 * Shouldn't be used for fragmented frames: such frames need to be reassembled first.
 *
 */
public fun Frame.Text.readText(): String {
    require(fin) { "Text could be only extracted from non-fragmented frame" }
    return Charsets.UTF_8.newDecoder().decode(buildPacket { writeFully(data) })
}

/**
 * Reads binary content from the frame. For fragmented frames only returns this fragment.
 *
 */
public fun Frame.readBytes(): ByteArray {
    return data.copyOf()
}

/**
 * Reads the close reason from the close frame or null if no close reason is provided.
 *
 */
public fun Frame.Close.readReason(): CloseReason? {
    if (data.size < 2) {
        return null
    }

    val packet = buildPacket { writeFully(data) }

    val code = packet.readShort()
    val message = packet.readText()

    return CloseReason(code, message)
}

internal data object NonDisposableHandle : DisposableHandle {
    override fun dispose() = Unit
}
