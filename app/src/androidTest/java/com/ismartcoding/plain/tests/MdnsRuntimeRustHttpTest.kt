package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.api.RustContentApi
import com.ismartcoding.plain.discover.RustMdnsRuntime
import kotlin.uuid.Uuid
import kotlin.uuid.ExperimentalUuidApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MdnsRuntimeRustHttpTest {
    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun closedPhysicalLeaseCannotAcquireAfterReleaseOrReplaceTheCurrentOwner() = runBlocking {
        RustMdnsRuntime.control("receiver")
        val closed = Uuid.random().toString()
        fun physical(acquire: Boolean, lease: String) = com.ismartcoding.plain.discover.MdnsMulticastHost.handle(buildJsonObject {
            put("acquire", acquire); put("lease", lease)
        }).boolean
        assertTrue(physical(false, closed))
        assertFalse(physical(true, closed))
        assertFalse(physical(true, Uuid.random().toString()))
        assertTrue(RustMdnsRuntime.control("receiver").getValue("receiver").jsonPrimitive.boolean)
    }

    @OptIn(ExperimentalUuidApi::class)
    @Test
    fun rustControlsReceiverAndScannerWithActualAndroidMulticastPermission() = runBlocking {
        suspend fun call(action: String) = RustContentApi.postJsonOrThrow("chat/mdns", buildJsonObject {
            put("action", action)
        }).getValue("result").jsonObject
        val before = call("snapshot")
        val debugToken = Uuid.random().toString()
        try {
            suspend fun discovering(): Boolean =
                RustContentApi.query("isDiscovering")["data"]!!.jsonObject["isDiscovering"]!!.jsonPrimitive.boolean
            assertEquals(true, RustContentApi.mutate("startDiscovery")["data"]!!.jsonObject["startDiscovery"]!!.jsonPrimitive.boolean)
            assertTrue(discovering())
            val started = call("snapshot")
            assertTrue(started.getValue("receiver").jsonPrimitive.boolean)
            assertTrue(started.getValue("scanning").jsonPrimitive.boolean)
            assertTrue(started.getValue("revision").jsonPrimitive.long > before.getValue("revision").jsonPrimitive.long)
            assertEquals(true, RustContentApi.mutate("stopDiscovery")["data"]!!.jsonObject["stopDiscovery"]!!.jsonPrimitive.boolean)
            assertFalse(discovering())
            val stopped = call("snapshot")
            assertFalse(stopped.getValue("scanning").jsonPrimitive.boolean)
            assertTrue(stopped.getValue("receiver").jsonPrimitive.boolean)
            assertTrue(stopped.getValue("revision").jsonPrimitive.long > started.getValue("revision").jsonPrimitive.long)
            assertTrue(call("snapshot").getValue("revision").jsonPrimitive.long >= stopped.getValue("revision").jsonPrimitive.long)
            RustMdnsRuntime.debugStart(debugToken)
            assertTrue(call("snapshot").getValue("capturing").jsonPrimitive.boolean)
            RustMdnsRuntime.control("browse")
            val captured = RustMdnsRuntime.snapshot()
            val packets = RustMdnsRuntime.packets(captured, false)
            assertEquals(captured.getValue("running").jsonPrimitive.boolean, RustMdnsRuntime.running)
            if (com.ismartcoding.plain.platform.getDeviceIP4s().isEmpty()) {
                assertTrue(packets.all { it.srcPort > 0 && it.size > 0 })
            } else assertTrue(packets.any { it.summary.contains("PTR") && it.detail.contains("_plainapp._tcp.local") && it.srcPort > 0 && it.size > 0 })
            assertTrue(RustMdnsRuntime.services(captured).all { it.serviceType == "_plainapp._tcp.local" })
            RustContentApi.mutate("stopDiscovery")
            assertTrue(discovering())
            RustMdnsRuntime.debugStop(debugToken)
            RustMdnsRuntime.control("receiver")
            val cleaned = call("snapshot")
            assertFalse(cleaned.getValue("scanning").jsonPrimitive.boolean)
            assertFalse(cleaned.getValue("capturing").jsonPrimitive.boolean)
            assertTrue(RustMdnsRuntime.packets(cleaned, false).isEmpty())
        } finally {
            RustMdnsRuntime.debugStop(debugToken)
            RustMdnsRuntime.control(if (before.getValue("manualScanning").jsonPrimitive.boolean) "start" else "stop")
        }
    }
}
