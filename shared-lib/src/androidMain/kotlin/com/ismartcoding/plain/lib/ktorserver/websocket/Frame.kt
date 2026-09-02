/*
 * Copyright 2014-2019 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
/**
 * Vendored from io.ktor:ktor-websockets:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.websocket

import io.ktor.util.*
import io.ktor.utils.io.core.*
import kotlinx.coroutines.*
import kotlinx.io.*
import java.nio.*

/**
 * A frame received or ready to be sent. It is not reusable and not thread-safe
 *
 *
 * @property fin is it final fragment, should be always `true` for control frames and if no fragmentation is used
 * @property frameType enum value
 * @property data - a frame content or fragment content
 * @property disposableHandle could be invoked when the frame is processed
 */

public sealed class Frame(
    public val fin: Boolean,
    public val frameType: FrameType,
    public val data: ByteArray,
    public val disposableHandle: DisposableHandle,
    public val rsv1: Boolean,
    public val rsv2: Boolean,
    public val rsv3: Boolean
) {
    init {
        validateSize()
    }

    /**
     * Frame content
     *
     */
    public val buffer: ByteBuffer = ByteBuffer.wrap(data)

    /**
     * Represents an application level binary frame.
     * In a RAW web socket session a big text frame could be fragmented
     * (separated into several text frames so they have [fin] = false except the last one).
     * Note that usually there is no need to handle fragments unless you have a RAW web socket session.
     *
     */
    public class Binary(
        fin: Boolean,
        data: ByteArray,
        rsv1: Boolean = false,
        rsv2: Boolean = false,
        rsv3: Boolean = false
    ) : Frame(fin, FrameType.BINARY, data, NonDisposableHandle, rsv1, rsv2, rsv3) {
        public constructor(fin: Boolean, buffer: ByteBuffer) : this(fin, buffer.moveToByteArray())

        public constructor(fin: Boolean, data: ByteArray) : this(fin, data, false, false, false)

        public constructor(fin: Boolean, packet: Source) : this(fin, packet.readByteArray())
    }

    /**
     * Represents an application level text frame.
     * In a RAW web socket session a big text frame could be fragmented
     * (separated into several text frames so they have [fin] = false except the last one).
     * Please note that a boundary between fragments could be in the middle of multi-byte (unicode) character
     * so don't apply String constructor to every fragment but use decoder loop instead of concatenate fragments first.
     * Note that usually there is no need to handle fragments unless you have a RAW web socket session.
     *
     */
    public class Text(
        fin: Boolean,
        data: ByteArray,
        rsv1: Boolean = false,
        rsv2: Boolean = false,
        rsv3: Boolean = false
    ) : Frame(fin, FrameType.TEXT, data, NonDisposableHandle, rsv1, rsv2, rsv3) {

        public constructor(fin: Boolean, data: ByteArray) : this(fin, data, false, false, false)

        public constructor(text: String) : this(true, text.toByteArray())

        public constructor(fin: Boolean, packet: Source) : this(fin, packet.readByteArray())

        public constructor(fin: Boolean, buffer: ByteBuffer) : this(fin, buffer.moveToByteArray())
    }

    /**
     * Represents a low-level level close frame. It could be sent to indicate web socket session end.
     * Usually there is no need to send/handle it unless you have a RAW web socket session.
     *
     */
    public class Close(
        data: ByteArray
    ) : Frame(true, FrameType.CLOSE, data, NonDisposableHandle, false, false, false) {

        public constructor(reason: CloseReason) : this(
            buildPacket {
                writeShort(reason.code)
                writeText(reason.message)
            }
        )

        public constructor(packet: Source) : this(packet.readByteArray())
        public constructor() : this(Empty)

        public constructor(buffer: ByteBuffer) : this(buffer.moveToByteArray())
    }

    /**
     * Represents a low-level ping frame. Could be sent to test connection (peer should reply with [Pong]).
     * Usually there is no need to send/handle it unless you have a RAW web socket session.
     *
     */
    public class Ping(
        data: ByteArray
    ) : Frame(true, FrameType.PING, data, NonDisposableHandle, false, false, false) {
        public constructor(packet: Source) : this(packet.readByteArray())
        public constructor(buffer: ByteBuffer) : this(buffer.moveToByteArray())
    }

    /**
     * Represents a low-level pong frame. Should be sent in reply to a [Ping] frame.
     * Usually there is no need to send/handle it unless you have a RAW web socket session.
     *
     */
    public class Pong(
        data: ByteArray,
        disposableHandle: DisposableHandle = NonDisposableHandle
    ) : Frame(true, FrameType.PONG, data, disposableHandle, false, false, false) {
        public constructor(packet: Source) : this(packet.readByteArray(), NonDisposableHandle)
        public constructor(
            buffer: ByteBuffer,
            disposableHandle: DisposableHandle = NonDisposableHandle
        ) : this(buffer.moveToByteArray(), disposableHandle)

        public constructor(buffer: ByteBuffer) : this(buffer.moveToByteArray(), NonDisposableHandle)
    }

    override fun toString(): String = "Frame $frameType (fin=$fin, buffer len = ${data.size})"

    /**
     * Creates a frame copy.
     *
     */
    public fun copy(): Frame = byType(fin, frameType, data.copyOf(), rsv1, rsv2, rsv3)

    public companion object {
        private val Empty: ByteArray = ByteArray(0)

        /**
         * Create a particular [Frame] instance by frame type.
         *
         */
        public fun byType(
            fin: Boolean,
            frameType: FrameType,
            data: ByteArray,
            rsv1: Boolean = false,
            rsv2: Boolean = false,
            rsv3: Boolean = false
        ): Frame = when (frameType) {
            FrameType.BINARY -> Binary(fin, data, rsv1, rsv2, rsv3)
            FrameType.TEXT -> Text(fin, data, rsv1, rsv2, rsv3)
            FrameType.CLOSE -> Close(data)
            FrameType.PING -> Ping(data)
            FrameType.PONG -> Pong(data, NonDisposableHandle)
        }
    }
}
