package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.ble.RustBleWire
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BleBinaryWireRustTest {
    @Test fun binaryJniKeepsEveryByteAcrossBothMtuSizes() {
        val body = ByteArray(8192) { it.toByte() }
        val message = RustBleWire.nearby(body)
        for (limit in listOf(20, 512)) {
            val assembly = RustBleWire.assembler()
            try {
                val id = RustBleWire.nextRequestId()
                val encoder = RustBleWire.encoder(message, id, true, limit)
                var received: ByteArray? = null
                try {
                    while (true) {
                        val frame = encoder.next() ?: break
                        assertTrue(frame.size <= limit)
                        assembly.push(frame)?.let { received = it }
                    }
                } finally { encoder.close() }
                assertEquals(id, assembly.requestId)
                assertTrue(assembly.response)
                assertArrayEquals(body, RustBleWire.nearbyBody(checkNotNull(received)))
            } finally { assembly.close() }
        }
    }
    @Test fun nativeCodecRejectsOutOfOrderAndReleasedHandle() {
        val assembly = RustBleWire.assembler()
        val data = RustBleWire.nearby(ByteArray(40))
        try {
            assembly.push(checkNotNull(RustBleWire.frame(data, 7, false, 0, 20)))
            try { assembly.push(checkNotNull(RustBleWire.frame(data, 7, false, 2, 20))); fail("Skipped fragment accepted") }
            catch (_: IllegalStateException) { }
        } finally { assembly.close() }
        try { assembly.push(ByteArray(0)); fail("Released handle accepted") }
        catch (_: IllegalStateException) { }
    }
}
