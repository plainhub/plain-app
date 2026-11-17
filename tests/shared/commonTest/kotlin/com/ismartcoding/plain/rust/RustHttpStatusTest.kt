package com.ismartcoding.plain.rust

import com.ismartcoding.plain.features.settings.ServiceState
import com.ismartcoding.plain.lib.JsonHelper
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RustHttpStatusTest {
    @Test
    fun rust_http_states_decode_directly_to_service_state_and_keep_the_wire_names() {
        for ((wire, expected) in listOf(
            "STARTED" to ServiceState.ON,
            "ERROR" to ServiceState.ERROR,
            "STOPPED" to ServiceState.OFF,
        )) {
            val status = JsonHelper.jsonDecode<RustHttpStatus>(
                """{"state":"$wire","message":"fixture","httpPort":8080,"httpsPort":8443}""",
            )
            assertEquals(expected, status.state)
            assertEquals("fixture", status.message)
            assertEquals(8080, status.httpPort)
            assertEquals(8443, status.httpsPort)
            val encoded = kotlinx.serialization.json.Json.parseToJsonElement(JsonHelper.jsonEncode(status))
            assertEquals(wire, encoded.jsonObject.getValue("state").jsonPrimitive.content)
        }
    }

    @Test
    fun unknown_http_states_are_rejected_at_the_protocol_boundary() {
        assertFailsWith<SerializationException> {
            JsonHelper.jsonDecode<RustHttpStatus>("""{"state":"unknown"}""")
        }
    }
}
