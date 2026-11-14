package com.ismartcoding.plain.models

import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.platform.system.DataType
import kotlinx.serialization.SerializationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SubscriptionDataTest {
    @Test
    fun media_changes_decode_graphql_enum_names_without_changing_numeric_domain_serialization() {
        val payload = """{"types":["AUDIO","VIDEO","IMAGE","DOC"]}"""
        val change = JsonHelper.jsonDecode<MediaChange>(payload)

        assertEquals(MediaDataType.entries.toList(), change.types)
        assertEquals(payload, JsonHelper.jsonEncode(change))
        for (type in DataType.entries) {
            val encoded = JsonHelper.jsonEncode(type)
            assertEquals(type.value.toString(), encoded)
            assertEquals(type, JsonHelper.jsonDecode<DataType>(encoded))
        }
    }

    @Test
    fun scan_progress_decodes_every_state_and_preserves_optional_fields() {
        for (state in FileIndexScanState.entries) {
            val progress = JsonHelper.jsonDecode<FileIndexScanProgress>(
                """{"indexed":8,"state":"${state.name}"}""",
            )
            assertEquals(FileIndexScanProgress(indexed = 8, state = state), progress)
            assertEquals("\"${state.name}\"", JsonHelper.jsonEncode(progress.state))
        }
        val progress = FileIndexScanProgress(8, 2, 10, FileIndexScanState.FAILED, "Scan failed")
        assertEquals(progress, JsonHelper.jsonDecode<FileIndexScanProgress>(JsonHelper.jsonEncode(progress)))
    }

    @Test
    fun unknown_types_and_states_are_rejected() {
        assertFailsWith<SerializationException> {
            JsonHelper.jsonDecode<MediaChange>("""{"types":["UNKNOWN"]}""")
        }
        assertFailsWith<SerializationException> {
            JsonHelper.jsonDecode<MediaChange>("""{"types":["NOTE"]}""")
        }
        assertFailsWith<SerializationException> {
            JsonHelper.jsonDecode<MediaChange>("""{"types":[3]}""")
        }
        assertFailsWith<SerializationException> {
            JsonHelper.jsonDecode<FileIndexScanProgress>("""{"indexed":0,"state":"UNKNOWN"}""")
        }
    }
}
