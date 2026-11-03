package com.ismartcoding.plain.tests

import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.rust.host.network.MulticastPermissionRequest
import kotlinx.serialization.Serializable
import com.ismartcoding.plain.rust.graphql.GraphqlApi
import com.ismartcoding.plain.rust.native.RustNativeApi

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.features.nearby.RustMdnsRuntime
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import com.ismartcoding.plain.rust.host.network.MdnsMulticastHost

@RunWith(AndroidJUnit4::class)
class MdnsRuntimeRustHttpTest {
    @Serializable
    private data class MulticastReply(val result: Boolean)

    private suspend fun awaitRunning() = withTimeout(10_000) {
        while (!RustMdnsRuntime.snapshot().running) delay(50)
    }
    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun closedPhysicalLeaseCannotAcquireAfterReleaseAndIndependentLeasePreservesRuntime() = runBlocking {
        awaitRunning()
        val closed = Uuid.random().toString()
        val before = RustMdnsRuntime.snapshot()
        fun physical(acquire: Boolean, lease: String) = JsonHelper.jsonDecode<MulticastReply>(
            MdnsMulticastHost.handle(JsonHelper.jsonEncodeToElement(MulticastPermissionRequest(lease, acquire)).jsonObject).encodeReply(0)
        ).result
        assertTrue(physical(false, closed))
        assertFalse(physical(true, closed))
        val independent = Uuid.random().toString()
        try {
            assertTrue(physical(true, independent))
            assertEquals(before.runtimeId, RustMdnsRuntime.snapshot().runtimeId)
            assertTrue(RustMdnsRuntime.snapshot().running)
        } finally { assertTrue(physical(false, independent)) }
        assertTrue(RustMdnsRuntime.snapshot().running)
    }

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun rustControlsReceiverAndScannerWithActualAndroidMulticastPermission() = runBlocking {
        awaitRunning()
        suspend fun snapshot() = RustMdnsRuntime.snapshot()
        val before = snapshot()
        val debugToken = Uuid.random().toString()
        val discoveryToken = Uuid.random().toString()
        val nestedDiscoveryToken = Uuid.random().toString()
        try {
            GraphqlApi.execute(Mutation.acquireDiscovery(discoveryToken))
            val started = snapshot()
            assertTrue(started.running)
            assertTrue(started.scanning)
            assertTrue(started.revision > before.revision)
            GraphqlApi.execute(Mutation.releaseDiscovery(discoveryToken))
            val stopped = snapshot()
            assertEquals(before.scanning, stopped.scanning)
            assertTrue(stopped.running)
            assertTrue(stopped.revision > started.revision)
            assertTrue(snapshot().revision >= stopped.revision)
            RustMdnsRuntime.debugStart(debugToken)
            assertTrue(snapshot().scanning)
            val captured = RustMdnsRuntime.snapshot(debug = true)
            val packets = captured.packetsOut
            assertEquals(captured.running, RustMdnsRuntime.running)
            if (RustNativeApi.lanIpv4Addresses().isEmpty()) {
                assertTrue(packets.all { it.srcPort > 0 && it.size > 0 })
            } else assertTrue(packets.any { it.summary.contains("PTR") && it.detail.contains("_plainapp._tcp.local") && it.srcPort > 0 && it.size > 0 })
            assertTrue(captured.services.all { it.serviceType == "_plainapp._tcp.local" })
            GraphqlApi.execute(Mutation.acquireDiscovery(nestedDiscoveryToken))
            GraphqlApi.execute(Mutation.releaseDiscovery(nestedDiscoveryToken))
            assertTrue(snapshot().scanning)
            RustMdnsRuntime.debugStop(debugToken).await()
            val cleaned = RustMdnsRuntime.snapshot(debug = true)
            assertEquals(before.scanning, cleaned.scanning)
            assertTrue(cleaned.packetsOut.isEmpty())
        } finally {
            RustMdnsRuntime.debugStop(debugToken).await()
            GraphqlApi.execute(Mutation.releaseDiscovery(discoveryToken))
            GraphqlApi.execute(Mutation.releaseDiscovery(nestedDiscoveryToken))
        }
    }
}
