package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.api.RustContentApi
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MdnsRuntimeRustHttpTest {
    @Test
    fun rustControlsReceiverAndScannerWithActualAndroidMulticastPermission() = runBlocking {
        suspend fun call(action: String) = RustContentApi.postJson("chat/mdns", buildJsonObject {
            put("action", action)
        }).getValue("result").jsonObject
        val before = call("snapshot")
        try {
            val started = call("start")
            assertTrue(started.getValue("receiver").jsonPrimitive.boolean)
            assertTrue(started.getValue("scanning").jsonPrimitive.boolean)
            assertTrue(started.getValue("revision").jsonPrimitive.long > before.getValue("revision").jsonPrimitive.long)
            val stopped = call("stop")
            assertFalse(stopped.getValue("scanning").jsonPrimitive.boolean)
            assertTrue(stopped.getValue("receiver").jsonPrimitive.boolean)
            assertTrue(stopped.getValue("revision").jsonPrimitive.long > started.getValue("revision").jsonPrimitive.long)
            assertTrue(call("snapshot").getValue("revision").jsonPrimitive.long >= stopped.getValue("revision").jsonPrimitive.long)
        } finally {
            call(if (before.getValue("scanning").jsonPrimitive.boolean) "start" else "stop")
        }
    }
}
