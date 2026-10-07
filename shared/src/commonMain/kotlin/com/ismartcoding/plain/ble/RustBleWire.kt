package com.ismartcoding.plain.ble

object RustBleWire : BleWire {
    override fun nextRequestId(): Int {
        val handle = RustBleBridge.create()
        RustBleBridge.free(handle)
        check(handle in 1..0xffffffffL) { "BLE request IDs exhausted" }
        return handle.toInt()
    }
    override fun encoder(message: ByteArray, requestId: Int, response: Boolean, valueLimit: Int): BleEncoder = RustBleEncoder(message, requestId, response, valueLimit)
    override fun frame(message: ByteArray, requestId: Int, response: Boolean, sequence: Int, valueLimit: Int): ByteArray? =
        RustBleBridge.call(2, 0, requestId, sequence, valueLimit, response, message)
    override fun nearby(body: ByteArray): ByteArray = checkNotNull(RustBleBridge.call(0, 0, 0, 0, 0, false, body))
    override fun nearbyBody(message: ByteArray): ByteArray = checkNotNull(RustBleBridge.call(1, 0, 0, 0, 0, false, message))
    override fun assembler(): BleAssembler = RustBleAssembler()
}
