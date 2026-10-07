package com.ismartcoding.plain.tests

import java.nio.ByteBuffer
import java.nio.ByteOrder

internal object BleBinaryFixture {
    fun peer(clientId: String, body: ByteArray): ByteArray = message(metadata(1, clientId, ""), body)
    fun file(clientId: String, fileId: String, offset: Long, length: Int): ByteArray {
        val prefix = metadata(2, clientId, fileId)
        return message(ByteBuffer.allocate(prefix.size + 12).order(ByteOrder.LITTLE_ENDIAN).put(prefix).putLong(offset).putInt(length).array(), ByteArray(0))
    }
    fun response(bytes: ByteArray): Pair<Int, ByteArray> {
        val buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        check(buffer.int == 2)
        val length = buffer.int
        val status = buffer.short.toInt() and 0xffff
        check(buffer.remaining() == length)
        return status to ByteArray(length).also { buffer.get(it) }
    }
    private fun metadata(operation: Int, first: String, second: String): ByteArray {
        val a = first.encodeToByteArray(); val b = second.encodeToByteArray()
        return ByteBuffer.allocate(5 + a.size + b.size).order(ByteOrder.LITTLE_ENDIAN)
            .put(operation.toByte()).putShort(a.size.toShort()).put(a).putShort(b.size.toShort()).put(b).array()
    }
    private fun message(metadata: ByteArray, body: ByteArray): ByteArray =
        ByteBuffer.allocate(8 + metadata.size + body.size).order(ByteOrder.LITTLE_ENDIAN)
            .putInt(metadata.size).putInt(body.size).put(metadata).put(body).array()
}
