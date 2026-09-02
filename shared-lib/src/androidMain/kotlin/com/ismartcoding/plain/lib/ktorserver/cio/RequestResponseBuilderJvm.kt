/*
 * Copyright 2014-2024 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */
/**
 * Vendored from io.ktor:ktor-http-cio:3.5.2.
 */

package com.ismartcoding.plain.lib.ktorserver.cio

import io.ktor.http.*
import io.ktor.utils.io.core.*
import kotlinx.io.Source
import java.nio.ByteBuffer

/**
 * Builds an HTTP request or response
 *
 */

public class RequestResponseBuilder() {
    private val packet = BytePacketBuilder()

    /**
     * Append response status line
     *
     */
    public fun responseLine(version: CharSequence, status: Int, statusText: CharSequence) {
        packet.writeText(version)
        packet.writeByte(SP)
        packet.writeText(status.toString())
        packet.writeByte(SP)
        packet.writeText(statusText)
        packet.writeByte(CR)
        packet.writeByte(LF)
    }

    /**
     * Append request line
     *
     */
    public fun requestLine(method: HttpMethod, uri: CharSequence, version: CharSequence) {
        packet.writeText(method.value)
        packet.writeByte(SP)
        packet.writeText(uri)
        packet.writeByte(SP)
        packet.writeText(version)
        packet.writeByte(CR)
        packet.writeByte(LF)
    }

    /**
     * Append a line
     *
     */
    public fun line(line: CharSequence) {
        packet.append(line)
        packet.writeByte(CR)
        packet.writeByte(LF)
    }

    /**
     * Append raw bytes
     *
     */
    public fun bytes(content: ByteArray, offset: Int = 0, length: Int = content.size) {
        packet.writeFully(content, offset, length)
    }

    /**
     * Append raw bytes
     *
     */
    public fun bytes(content: ByteBuffer) {
        packet.writeFully(content)
    }

    /**
     * Append header line
     *
     */
    public fun headerLine(name: CharSequence, value: CharSequence) {
        packet.append(name)
        packet.append(": ")
        packet.append(value)
        packet.writeByte(CR)
        packet.writeByte(LF)
    }

    /**
     * Append an empty line (CR + LF in fact)
     *
     */
    public fun emptyLine() {
        packet.writeByte(CR)
        packet.writeByte(LF)
    }

    /**
     * Build a packet of request/response
     *
     */
    public fun build(): Source = packet.build()

    /**
     * Release all resources hold by the builder
     *
     */
    public fun release() {
        packet.close()
    }
}

private const val SP: Byte = 0x20
private const val CR: Byte = 0x0d
private const val LF: Byte = 0x0a
