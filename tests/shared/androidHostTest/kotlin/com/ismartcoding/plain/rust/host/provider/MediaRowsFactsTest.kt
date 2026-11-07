package com.ismartcoding.plain.rust.host.provider

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.media.MediaVolumeRevision
import com.ismartcoding.plain.rust.host.hostResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.serialization.json.*

class MediaRowsFactsTest {
    @Test
    fun revision_samples_keep_long_generations_and_original_rows() {
        val row = JsonHelper.jsonDecode<JsonElement>("""{"id":"track","durationMs":1234}""")
        val revision = listOf(MediaVolumeRevision("external_primary", "version", 3_000_000_123L))
        val facts = MediaRowsFacts(listOf(hostResult(row)), revision, revision)
        val encoded = JsonHelper.jsonDecode<JsonObject>(hostResult(facts).encodeReply(1)).getValue("result").jsonObject
        assertEquals(row, encoded.getValue("rows").jsonArray.single())
        assertEquals(3_000_000_123L, encoded.getValue("revisionBefore").jsonArray.single().jsonObject.getValue("generation").jsonPrimitive.long)
        assertEquals(encoded.getValue("revisionBefore"), encoded.getValue("revisionAfter"))
    }

    @Test
    fun unavailable_revision_stays_explicitly_unavailable() {
        val facts = MediaRowsFacts(emptyList(), null, null)
        assertEquals("""{"id":1,"result":{"rows":[],"revisionBefore":null,"revisionAfter":null}}""", hostResult(facts).encodeReply(1))
    }
}
