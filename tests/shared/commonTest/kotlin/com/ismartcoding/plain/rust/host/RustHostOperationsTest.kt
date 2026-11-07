package com.ismartcoding.plain.rust.host

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.network.ble.PairingTransport
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

class RustHostOperationsTest {
    @Test
    fun retiredAndUnknownMethodsAreRejected() = runTest {
        val advertising = PairingTransport.connect { error("BLE is not requested") }
        try {
            for (method in listOf("systemVpnActive", "systemMemoryAvailable", "systemUpdateScreenMirrorQuality", "fileMetadataFacts", "unknown")) {
                assertFailsWith<IllegalStateException>(method) {
                    executeHostRequest(RustHostRequest(1, method, JsonHelper.jsonDecode("{}")), this, advertising)
                }
            }
        } finally { advertising.close() }
    }
}
