package com.ismartcoding.plain.tests

import com.ismartcoding.plain.rust.graphql.GraphqlRequest
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.native.RustNativeApi

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.features.nearby.RustMdnsRuntime
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import com.ismartcoding.plain.rust.host.network.MdnsMulticastHost

@RunWith(AndroidJUnit4::class)
class MdnsRuntimeRustHttpTest {
    private suspend fun awaitReceiver() = withTimeout(10_000) {
        while (!RustMdnsRuntime.snapshot().receiver) delay(50)
    }
    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun closedPhysicalLeaseCannotAcquireAfterReleaseOrReplaceTheCurrentOwner() = runBlocking {
        awaitReceiver()
        val closed = Uuid.random().toString()
        fun physical(acquire: Boolean, lease: String) = MdnsMulticastHost.handle(buildJsonObject {
            put("acquire", acquire); put("lease", lease)
        }).encodeReply(0).let { Json.parseToJsonElement(it).jsonObject.getValue("result").jsonPrimitive.boolean }
        assertTrue(physical(false, closed))
        assertFalse(physical(true, closed))
        assertFalse(physical(true, Uuid.random().toString()))
        assertTrue(RustMdnsRuntime.snapshot().receiver)
    }

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun rustControlsReceiverAndScannerWithActualAndroidMulticastPermission() = runBlocking {
        awaitReceiver()
        suspend fun snapshot() = RustMdnsRuntime.snapshot()
        val before = snapshot()
        val debugToken = Uuid.random().toString()
        try {
            suspend fun discovering(): Boolean =
                GraphqlApi.execute(GraphqlRequest<JsonObject>("query { isDiscovering }")).getValue("isDiscovering").jsonPrimitive.boolean
            assertEquals(true, GraphqlApi.execute(GraphqlRequest<JsonObject>("mutation { startDiscovery }")).getValue("startDiscovery").jsonPrimitive.boolean)
            assertTrue(discovering())
            val started = snapshot()
            assertTrue(started.receiver)
            assertTrue(started.scanning)
            assertTrue(started.revision > before.revision)
            assertEquals(true, GraphqlApi.execute(GraphqlRequest<JsonObject>("mutation { stopDiscovery }")).getValue("stopDiscovery").jsonPrimitive.boolean)
            assertFalse(discovering())
            val stopped = snapshot()
            assertFalse(stopped.scanning)
            assertTrue(stopped.receiver)
            assertTrue(stopped.revision > started.revision)
            assertTrue(snapshot().revision >= stopped.revision)
            RustMdnsRuntime.debugStart(debugToken)
            assertTrue(snapshot().capturing)
            val captured = RustMdnsRuntime.snapshot()
            val packets = captured.packetsOut
            assertEquals(captured.running, RustMdnsRuntime.running)
            if (RustNativeApi.lanIpv4Addresses().isEmpty()) {
                assertTrue(packets.all { it.srcPort > 0 && it.size > 0 })
            } else assertTrue(packets.any { it.summary.contains("PTR") && it.detail.contains("_plainapp._tcp.local") && it.srcPort > 0 && it.size > 0 })
            assertTrue(captured.services.all { it.serviceType == "_plainapp._tcp.local" })
            GraphqlApi.execute(GraphqlRequest<JsonObject>("mutation { stopDiscovery }"))
            assertTrue(discovering())
            RustMdnsRuntime.debugStop(debugToken).await()
            val cleaned = snapshot()
            assertFalse(cleaned.scanning)
            assertFalse(cleaned.capturing)
            assertTrue(cleaned.packetsOut.isEmpty())
        } finally {
            RustMdnsRuntime.debugStop(debugToken).await()
            GraphqlApi.execute(GraphqlRequest<JsonObject>("mutation { " + (if (before.manualScanning) "startDiscovery" else "stopDiscovery") + " }"))
        }
    }
}
