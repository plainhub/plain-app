package com.ismartcoding.plain.ble

internal expect object RustBleBridge {
    fun encoder(data: ByteArray, requestId: Int, response: Boolean, limit: Int): Long
    fun create(): Long
    fun free(handle: Long)
    fun info(handle: Long): Long
    fun call(action: Int, handle: Long, requestId: Int, sequence: Int, limit: Int, response: Boolean, data: ByteArray): ByteArray?
}
