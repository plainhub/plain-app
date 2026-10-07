package com.ismartcoding.plain.ble

import com.ismartcoding.plain.platform.PlatformLock

internal class RustBleAssembler : BleAssembler {
    private val lock = PlatformLock()
    private var handle = RustBleBridge.create()
    override val requestId: Int get() = lock.withLock { RustBleBridge.info(handle).toInt() }
    override val response: Boolean get() = lock.withLock { RustBleBridge.info(handle) ushr 32 != 0L }
    override fun push(frame: ByteArray): ByteArray? = lock.withLock {
        check(handle != 0L) { "BLE assembler closed" }
        RustBleBridge.call(3, handle, 0, 0, 0, false, frame)
    }
    override fun close() = lock.withLock { if (handle != 0L) { RustBleBridge.free(handle); handle = 0 } }
}
