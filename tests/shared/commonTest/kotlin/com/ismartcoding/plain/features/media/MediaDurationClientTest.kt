package com.ismartcoding.plain.features.media

import com.ismartcoding.plain.rust.graphql.graphqlClient
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.*
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue

class MediaDurationClientTest {
    @Test
    fun duration_uses_one_path_only_file_info_request_and_only_duration() = runTest {
        val path = "app://unknown \"name\".mp3"
        var calls = 0
        val duration = readMediaDurationMs(
            path,
            api = graphqlClient(
                query = { request ->
                    calls += 1
                    assertEquals("query(\$path:String!){fileInfo(path:\$path){data{... on AudioFileInfo{durationMs} ... on VideoFileInfo{durationMs}}}}", request.query)
                    assertEquals(mapOf("path" to JsonPrimitive(path)), request.variables)
                    JsonObject(mapOf("fileInfo" to JsonObject(mapOf("data" to JsonObject(mapOf("durationMs" to JsonPrimitive(6000L)))))))
                },
            ),
        )
        assertEquals(6000L, duration)
        assertEquals(1, calls)
    }

    @Test
    fun missing_or_unsupported_metadata_and_real_zero_duration_return_zero() = runTest {
        for (data in listOf(JsonNull, JsonObject(mapOf("durationMs" to JsonPrimitive(0L))))) {
            val duration = readMediaDurationMs(
                "missing",
                api = graphqlClient(
                    query = { _ ->
                        JsonObject(mapOf("fileInfo" to JsonObject(mapOf("data" to data))))
                    },
                ),
            )
            assertEquals(0L, duration)
        }
    }

    @Test
    fun missing_file_info_is_nullable() = runTest {
        val duration = readMediaDurationMs(
            "app://missing.mp3",
            api = graphqlClient(query = { JsonObject(mapOf("fileInfo" to JsonNull)) }),
        )
        assertEquals(0L, duration)
    }

    @Test
    fun request_errors_propagate_without_retries_or_a_fabricated_duration() = runTest {
        val failure = IllegalStateException("no_permission")
        var calls = 0
        val received = assertFailsWith<IllegalStateException> {
            readMediaDurationMs(
                "denied",
                api = graphqlClient(
                    query = { _ ->
                        calls += 1
                        throw failure
                    },
                ),
            )
        }
        assertSame(failure, received)
        assertEquals(1, calls)
    }

    @Test
    fun malformed_success_payload_is_not_treated_as_unknown_media() = runTest {
        val failure = assertFailsWith<SerializationException> {
            readMediaDurationMs(
                "broken",
                api = graphqlClient(
                    query = { _ ->
                        JsonObject(mapOf("fileInfo" to JsonObject(mapOf("data" to JsonObject(emptyMap())))))
                    },
                ),
            )
        }
        assertTrue(failure.message.orEmpty().contains("durationMs"))
    }
}
