/*
 * Copyright 2014-2025 JetBrains s.r.o and contributors. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ismartcoding.plain.lib.ktorserver.core.http.content

import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.utils.io.*
import java.io.File
import java.io.FileInputStream

private const val FILE_REGION_FALLBACK_BUFFER_SIZE = 64 * 1024
private const val FILE_REGION_FALLBACK_FLUSH_BYTES = 64 * 1024

/**
 * Outgoing content representing a byte range of a local [file].
 *
 * The Netty engine serves it through a [io.netty.channel.FileRegion] (kernel
 * sendfile on plain-text ports, chunked encryption behind an [javax.net.ssl.SSLContext]
 * based SslHandler), so file bytes never pass through a heap buffer. Engines without
 * region support fall back to the buffered [writeTo] implementation.
 */
public class FileRegionContent(
    public val file: File,
    public val offset: Long,
    public val length: Long,
    override val contentType: ContentType? = null,
    override val status: HttpStatusCode? = null,
    override val headers: Headers = Headers.Empty,
) : OutgoingContent.WriteChannelContent() {

    init {
        require(offset >= 0) { "offset must be non-negative" }
        require(length >= 0) { "length must be non-negative" }
    }

    override val contentLength: Long get() = length

    override suspend fun writeTo(channel: ByteWriteChannel) {
        if (length <= 0) {
            channel.flush()
            return
        }

        FileInputStream(file).use { input ->
            input.channel.position(offset)
            val buffer = ByteArray(FILE_REGION_FALLBACK_BUFFER_SIZE)
            var remaining = length
            var bytesSinceFlush = 0

            while (remaining > 0) {
                val readLength = minOf(buffer.size.toLong(), remaining).toInt()
                val read = input.read(buffer, 0, readLength)
                if (read <= 0) break

                channel.writeFully(buffer, 0, read)
                remaining -= read
                bytesSinceFlush += read

                if (bytesSinceFlush >= FILE_REGION_FALLBACK_FLUSH_BYTES) {
                    channel.flush()
                    bytesSinceFlush = 0
                }
            }
            channel.flush()
        }
    }
}
