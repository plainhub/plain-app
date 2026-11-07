package com.ismartcoding.plain.rust.host

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.network.ble.PairingTransport
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RustAndroidHostOperationsTest {
    @Test
    fun providerRequestsReachTheirTypedDecoders() = runTest {
        val advertising = PairingTransport.connect { error("BLE is not requested") }
        try {
            for (method in listOf("systemFileIndexRows", "systemMediaRows", "systemMakeCall", "systemReplyNotification")) {
                assertFailsWith<SerializationException>(method) {
                    executeHostRequest(RustHostRequest(1, method, JsonHelper.jsonDecode("{}")), this, advertising)
                }
            }
        } finally { advertising.close() }
    }

    @Test
    fun emptyScreenMirrorInputsCompleteWithoutAnOsCall() = runTest {
        val advertising = PairingTransport.connect { error("BLE is not requested") }
        try {
            val request = RustHostRequest(1, "systemScreenMirrorControls", JsonHelper.jsonDecode("""{"inputs":[]}"""))
            assertEquals("""{"id":1,"result":null}""", executeHostRequest(request, this, advertising).encodeReply(1))
        } finally { advertising.close() }
    }
}
