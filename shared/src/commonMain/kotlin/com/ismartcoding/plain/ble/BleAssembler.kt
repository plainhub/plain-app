package com.ismartcoding.plain.ble

interface BleAssembler : AutoCloseable {
    val requestId: Int
    val response: Boolean
    fun push(frame: ByteArray): ByteArray?
}
