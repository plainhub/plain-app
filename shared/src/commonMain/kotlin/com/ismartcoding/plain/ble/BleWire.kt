package com.ismartcoding.plain.ble

interface BleWire {
    fun nextRequestId(): Int
    fun encoder(message: ByteArray, requestId: Int, response: Boolean, valueLimit: Int): BleEncoder
    fun frame(message: ByteArray, requestId: Int, response: Boolean, sequence: Int, valueLimit: Int): ByteArray?
    fun nearby(body: ByteArray): ByteArray
    fun nearbyBody(message: ByteArray): ByteArray
    fun assembler(): BleAssembler
}
