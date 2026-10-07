package com.ismartcoding.plain.ble

internal actual object RustBleBridge {
    init { System.loadLibrary("plain_rust") }
    private external fun encoderNative(data: ByteArray, requestId: Int, response: Boolean, limit: Int): Long
    actual fun encoder(data: ByteArray, requestId: Int, response: Boolean, limit: Int): Long = encoderNative(data, requestId, response, limit)
    private external fun newNative(): Long
    private external fun freeNative(handle: Long)
    private external fun infoNative(handle: Long): Long
    private external fun callNative(action: Int, handle: Long, requestId: Int, sequence: Int, limit: Int, response: Boolean, data: ByteArray): ByteArray?
    actual fun create(): Long = newNative()
    actual fun free(handle: Long) = freeNative(handle)
    actual fun info(handle: Long): Long = infoNative(handle)
    actual fun call(action: Int, handle: Long, requestId: Int, sequence: Int, limit: Int, response: Boolean, data: ByteArray): ByteArray? =
        callNative(action, handle, requestId, sequence, limit, response, data)
}
