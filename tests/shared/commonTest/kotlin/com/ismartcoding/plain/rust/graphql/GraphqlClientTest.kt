package com.ismartcoding.plain.rust.graphql

import com.ismartcoding.plain.features.files.FileSortBy
import com.ismartcoding.plain.platform.system.DataType
import com.ismartcoding.plain.models.Audio
import com.ismartcoding.plain.models.Image
import com.ismartcoding.plain.models.Video
import com.ismartcoding.plain.models.Doc
import com.ismartcoding.plain.graphql.Mutation
import com.ismartcoding.plain.graphql.Query
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.lib.logcat.LogAdapter
import com.ismartcoding.plain.lib.logcat.LogCat
import com.ismartcoding.plain.models.AudioItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.JsonObject
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlin.test.assertNull
import kotlin.test.assertSame

class GraphqlClientTest {
    @Test
    fun request_logs_report_transport_result_and_cancellation_without_payloads() = runTest {
        val logs = mutableListOf<Pair<Int, String>>()
        LogCat.addLogAdapter(object : LogAdapter {
            override fun isLoggable(priority: Int, tag: String?) = true
            override fun log(priority: Int, tag: String?, message: String) { logs += priority to message }
        })
        try {
            val request = GraphqlRequest<String>(
                "query DeviceName { deviceName }", operationName = "DeviceName", field = "deviceName",
                variables = mapOf(variable("secret", "private-variable")),
            )
            val client = GraphqlClient("http") {
                JsonHelper.jsonDecode<GraphqlResponse>("""{"data":{"deviceName":"private-response"}}""")
            }
            assertEquals("private-response", client.execute(request))
            val cancellation = CancellationException("private-cancellation")
            val cancelled = GraphqlClient { throw cancellation }
            assertSame(cancellation, assertFailsWith<CancellationException> { cancelled.execute(request) })
            val failure = IllegalStateException("private-error")
            val failed = GraphqlClient { throw failure }
            assertSame(failure, assertFailsWith<IllegalStateException> { failed.execute(request) })
            assertEquals(listOf(LogCat.DEBUG, LogCat.DEBUG, LogCat.ERROR), logs.map { it.first })
            assertTrue(logs[0].second.startsWith("GraphQL transport=http operation=DeviceName result=success elapsedMs="))
            assertTrue(logs[1].second.startsWith("GraphQL transport=native operation=DeviceName result=cancelled elapsedMs="))
            assertTrue(logs[2].second.startsWith("GraphQL transport=native operation=DeviceName result=error error=IllegalStateException elapsedMs="))
            assertTrue(logs.none { it.second.contains("private-") || it.second.contains("query DeviceName") })
        } finally {
            LogCat.clearLogAdapters()
        }
    }

    @Test
    fun one_execution_selects_and_decodes_the_request_result() = runTest {
        val request = Query.audioQueueItems(0, 10, "")
        var calls = 0
        val api = GraphqlClient { received ->
            calls++
            assertSame(request.operation, received)
            JsonHelper.jsonDecode<GraphqlResponse>(
                """{"data":{"audioQueueItems":[{"mediaId":"track","title":"Song","artist":"Artist","path":"/music/song.mp3","durationMs":3000000123}],"unrelated":null},"errors":[]}""",
            )
        }

        val items = api.execute(request)
        assertEquals(listOf(AudioItem("track", "Song", "Artist", "/music/song.mp3", 3_000_000_123)), items)
        assertEquals(1, calls)
    }

    @Test
    fun unit_execution_checks_errors_without_decoding_an_unused_value() = runTest {
        val request = Mutation.startPomodoro(60)
        var calls = 0
        val api = GraphqlClient {
            calls++
            GraphqlResponse(data = JsonObject(emptyMap()))
        }
        assertEquals(Unit, api.execute(request))
        assertEquals(1, calls)

        val failed = GraphqlClient {
            GraphqlResponse(data = JsonObject(emptyMap()), errors = listOf(GraphqlError("no_permission")))
        }
        assertEquals("no_permission", assertFailsWith<IllegalStateException> {
            failed.execute(request)
        }.message)
    }

    @Test
    fun composite_requests_decode_the_complete_response() = runTest {
        val api = GraphqlClient {
            JsonHelper.jsonDecode<GraphqlResponse>("""{"data":{"message0":null}}""")
        }
        val snapshot = api.execute(Query.chatSnapshot(listOf("missing")))
        assertEquals(mapOf("message0" to null), snapshot)
    }

    @Test
    fun null_results_missing_fields_and_invalid_rows_remain_distinct() = runTest {
        val request = Query.chatItem("missing")
        val nullable = GraphqlClient {
            JsonHelper.jsonDecode<GraphqlResponse>("""{"data":{"chatItem":null}}""")
        }
        assertNull(nullable.execute(request))

        val missing = GraphqlClient { GraphqlResponse(data = JsonObject(emptyMap())) }
        assertFailsWith<NoSuchElementException> { missing.execute(request) }

        val invalid = GraphqlClient {
            JsonHelper.jsonDecode<GraphqlResponse>("""{"data":{"audioQueueItems":[{}]}}""")
        }
        assertFailsWith<SerializationException> {
            invalid.execute(Query.audioQueueItems(0, 10, ""))
        }
        val noData = GraphqlClient { GraphqlResponse() }
        assertFailsWith<IllegalStateException> { noData.execute(Mutation.startPomodoro(60)) }
    }

    @Test
    fun media_requests_choose_the_concrete_decoder_before_execution() = runTest {
        val row = """{"id":"media","title":"Title","artist":"Artist","path":"/media/file","size":3000000123,"durationMs":4000000123,"bucketId":"bucket","takenAt":null,"createdAt":"2026-01-01T00:00:00Z","updatedAt":"2026-01-01T00:00:00Z"}"""
        val api = GraphqlClient {
            JsonHelper.jsonDecode<GraphqlResponse>("""{"data":{"audios":[$row],"images":[$row],"videos":[$row],"docs":[$row]}}""")
        }
        for (type in listOf(DataType.AUDIO, DataType.IMAGE, DataType.VIDEO, DataType.DOC)) {
            val item = api.execute(Query.mediaRows(type, "", 10, 0, FileSortBy.NAME_ASC)).single()
            assertTrue(when (type) {
                DataType.AUDIO -> item is Audio
                DataType.IMAGE -> item is Image
                DataType.VIDEO -> item is Video
                DataType.DOC -> item is Doc
                else -> false
            })
            assertEquals("media", item.id)
        }
    }

    @Test
    fun saved_values_infer_the_result_from_the_default_and_only_default_on_null() = runTest {
        val api = GraphqlClient {
            JsonHelper.jsonDecode<GraphqlResponse>("""{"data":{"savedValue":3000000123}}""")
        }
        val value = api.execute(Query.savedValue("key", 0L))
        assertEquals(3_000_000_123L, value)
        val absent = GraphqlClient {
            JsonHelper.jsonDecode<GraphqlResponse>("""{"data":{"savedValue":null}}""")
        }
        assertEquals(7L, absent.execute(Query.savedValue("key", 7L)))
        for (value in listOf("{}", "[]", "true", "\"not-a-number\"")) {
            val invalid = GraphqlClient {
                GraphqlResponse(data = JsonObject(mapOf("savedValue" to JsonHelper.jsonDecode(value))))
            }
            assertFailsWith<Exception> { invalid.execute(Query.savedValue("key", 7L)) }
        }
    }

    @Test
    fun scalar_requests_return_their_values_without_exposing_json() = runTest {
        val api = GraphqlClient {
            JsonHelper.jsonDecode<GraphqlResponse>("""{"data":{"appLogFilesSize":3000000123,"appCacheSize":4000000123,"clearAppCache":true,"exportAppLogs":null}}""")
        }
        val logSize = api.execute(Query.appLogFilesSize())
        val cacheSize = api.execute(Query.appCacheSize())
        val cleared = api.execute(Mutation.clearAppCache())
        val archive = api.execute(Mutation.exportAppLogs())
        assertEquals(3_000_000_123L, logSize)
        assertEquals(4_000_000_123L, cacheSize)
        assertTrue(cleared)
        assertNull(archive)
    }

    @Test
    fun request_failures_and_cancellation_propagate_without_retries() = runTest {
        val request = Query.audioQueueItemCount()
        var calls = 0
        val failure = IllegalStateException("Request failed")
        val failed = GraphqlClient { calls++; throw failure }
        assertSame(failure, assertFailsWith<IllegalStateException> { failed.execute(request) })
        assertEquals(1, calls)

        val cancellation = CancellationException("Request cancelled")
        val cancelled = GraphqlClient { calls++; throw cancellation }
        assertSame(cancellation, assertFailsWith<CancellationException> { cancelled.execute(request) })
        assertEquals(2, calls)
    }
}
