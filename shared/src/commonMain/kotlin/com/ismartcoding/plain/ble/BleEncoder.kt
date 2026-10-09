package com.ismartcoding.plain.ble

interface BleEncoder : AutoCloseable {
    fun next(): ByteArray?
}
