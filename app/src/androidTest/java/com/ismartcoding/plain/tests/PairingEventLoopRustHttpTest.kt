package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.data.DPairingCancel
import com.ismartcoding.plain.data.DPairingRequest
import com.ismartcoding.plain.discover.RustPairingRuntime
import com.ismartcoding.plain.enums.DeviceType
import com.ismartcoding.plain.events.PairingCanceledEvent
import com.ismartcoding.plain.events.PairingRequestReceivedEvent
import com.ismartcoding.plain.events.WebSocketData
import com.ismartcoding.plain.events.WebSocketEvent
import com.ismartcoding.plain.lib.Channel
import com.ismartcoding.plain.platform.generateECDHKeyPair
import com.ismartcoding.plain.platform.generateEd25519KeyPair
import com.ismartcoding.plain.platform.signEd25519
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID
import java.util.concurrent.atomic.AtomicInteger
import kotlin.io.encoding.Base64

@RunWith(AndroidJUnit4::class)
class PairingEventLoopRustHttpTest {
    @Test
    fun requestCancelRequestDoesNotFeedEventsBackIntoRust() = runBlocking {
        val id = "synthetic-pairing-loop-${UUID.randomUUID()}"
        val requests = AtomicInteger()
        val cancels = AtomicInteger()
        val republished = AtomicInteger()
        val observer = launch(start = CoroutineStart.UNDISPATCHED) {
            Channel.sharedFlow.collect { event ->
                when (event) {
                    is PairingRequestReceivedEvent -> if (event.request.fromId == id) requests.incrementAndGet()
                    is PairingCanceledEvent -> if (event.fromId == id) cancels.incrementAndGet()
                    is WebSocketEvent -> if ((event.data as? WebSocketData.Text)?.value?.contains(id) == true) republished.incrementAndGet()
                }
            }
        }
        suspend fun awaitCount(counter: AtomicInteger, count: Int) {
            withTimeout(10_000) { while (counter.get() < count) delay(20) }
        }
        try {
            val first = invitation(id)
            RustPairingRuntime.receiveRequest(first, "synthetic-mac", true)
            awaitCount(requests, 1)
            RustPairingRuntime.receiveCancel(DPairingCancel(id, TempData.clientId))
            awaitCount(cancels, 1)
            val second = invitation(id)
            RustPairingRuntime.receiveRequest(second, "synthetic-mac", true)
            awaitCount(requests, 2)
            RustPairingRuntime.receiveRequest(second, "synthetic-mac", true)
            delay(1_000)
            assertEquals("Each invitation must reach the UI once", 2, requests.get())
            assertEquals("Old cancellations must not close the replacement invitation", 1, cancels.get())
            assertEquals("Rust events must not be published back by mobile projections", 0, republished.get())
        } finally {
            observer.cancel()
            RustPairingRuntime.receiveCancel(DPairingCancel(id, TempData.clientId))
        }
    }

    private suspend fun invitation(id: String): DPairingRequest {
        val ecdh = generateECDHKeyPair()
        val (privateKey, publicKey) = generateEd25519KeyPair()
        return DPairingRequest(
            fromId = id, fromName = id, port = 2443, deviceType = DeviceType.PHONE,
            ecdhPublicKey = Base64.encode(ecdh.publicKeyEncoded),
            signaturePublicKey = Base64.encode(publicKey), timestamp = System.currentTimeMillis(), ips = emptyList(),
        ).also { request ->
            request.signature = Base64.encode(signEd25519(privateKey, request.toSignatureData().encodeToByteArray()))
        }
    }
}
