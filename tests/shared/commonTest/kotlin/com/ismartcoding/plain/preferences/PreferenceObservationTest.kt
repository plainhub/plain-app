package com.ismartcoding.plain.preferences

import com.ismartcoding.plain.Prefs
import kotlinx.serialization.json.JsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonObject

class PreferenceObservationTest {
    @Test
    fun typedUpdatesChangeOnlyTheirOwnPreference() {
        val oldPort = Prefs.httpPort.value
        val oldHttps = Prefs.https.value
        try {
            Prefs.update(PrefKeys.HTTP_PORT, JsonPrimitive(8080))
            assertEquals(8080, Prefs.httpPort.value)
            assertEquals(oldHttps, Prefs.https.value)
            assertFailsWith<IllegalArgumentException> { Prefs.update(PrefKeys.HTTP_PORT, JsonPrimitive("invalid")) }
            assertEquals(8080, Prefs.httpPort.value)
        } finally { Prefs.httpPort.value = oldPort }
    }
    @Test
    fun successfulWritesReloadCanonicalProjectionEvenWhenStorageIsUnchanged() = runTest {
        val oldName = Prefs.deviceName.value
        var saved = JsonPrimitive("canonical")
        var reads = 0
        try {
            Prefs.deviceName.value = "stale"
            assertFalse(Prefs.set(PrefKeys.DEVICE_NAME, " raw ", persist = { key, value ->
                assertEquals(PrefKeys.DEVICE_NAME, key)
                assertEquals(JsonPrimitive(" raw "), value)
                false
            }, reload = {
                reads++
                Prefs.load(JsonObject(mapOf(PrefKeys.DEVICE_NAME to saved)))
            }))
            assertEquals("canonical", Prefs.deviceName.value)
            saved = JsonPrimitive("updated")
            Prefs.set(PrefKeys.DEVICE_NAME, "updated", persist = { _, _ -> true }, reload = {
                reads++
                Prefs.load(JsonObject(mapOf(PrefKeys.DEVICE_NAME to saved)))
            })
            assertEquals("updated", Prefs.deviceName.value)
            assertEquals(2, reads)
        } finally { Prefs.deviceName.value = oldName }
    }

    @Test
    fun failedWritesPreserveProjectionWithoutReading() = runTest {
        val oldName = Prefs.deviceName.value
        var reads = 0
        try {
            Prefs.deviceName.value = "preserved"
            assertFailsWith<IllegalStateException> {
                Prefs.set(PrefKeys.DEVICE_NAME, "rejected", persist = { _, _ -> error("rejected") }, reload = {
                    reads++
                    Prefs.load(JsonObject(mapOf(PrefKeys.DEVICE_NAME to JsonPrimitive("rejected"))))
                })
            }
            assertEquals(0, reads)
            assertEquals("preserved", Prefs.deviceName.value)
        } finally { Prefs.deviceName.value = oldName }
    }

}
