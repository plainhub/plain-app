package com.ismartcoding.plain.features.web

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.AppAccess
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails

class CallsAccessFeatureTest {
    @Test
    fun callsUsesThePublicWireName() {
        val value = JsonHelper.jsonDecode<AppAccess>("""{"access":["CALLS"]}""")
        assertEquals(listOf(AccessFeature.CALLS), value.access)
        assertEquals("\"CALLS\"", JsonHelper.jsonEncode(AccessFeature.CALLS))
        for (name in listOf("CALL_LOGS", "UNKNOWN")) {
            assertFails { JsonHelper.jsonDecode<AccessFeature>("\"$name\"") }
        }
    }
}
