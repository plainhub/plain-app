package com.ismartcoding.plain.features.downloads

import com.ismartcoding.plain.features.downloads.DownloadCenter
import com.ismartcoding.plain.features.downloads.DownloadStatus
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.*
import com.ismartcoding.plain.rust.graphql.GraphqlClient
import com.ismartcoding.plain.rust.graphql.GraphqlOperation
import com.ismartcoding.plain.rust.graphql.GraphqlResponse
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class DownloadsClientTest {
    private val url = "https://fixture.invalid/fs?id=s.share.AA&action=download"
    private fun task(id: String) = Download(id, "Target download", ResourceDownloadSource(listOf(url)), "/target-downloads", null, DownloadStatus.DOWNLOADING, 4, 8, 4, 1, 0, "target.txt", "", emptyList())

    @Test
    fun complete_snapshots_replace_previous_and_empty_state_without_affecting_other_clients() = runTest {
        val local = DownloadCenter.progress.value
        var response = listOf(task("first"))
        val client = DownloadsClient(GraphqlClient {
            GraphqlResponse(data = JsonHelper.jsonEncodeToElement(mapOf("downloads" to response)).jsonObject)
        }, backgroundScope)
        client.refresh()
        assertEquals(setOf("first"), client.progress.value.keys)
        assertEquals(4L, client.progress.value.getValue("first").downloadedBytes)
        response = listOf(task("restarted"))
        client.refresh()
        assertEquals(setOf("restarted"), client.progress.value.keys)
        response = emptyList()
        client.refresh()
        assertTrue(client.progress.value.isEmpty())
        assertEquals(local, DownloadCenter.progress.value)
    }

    @Test
    fun creation_and_all_controls_use_the_supplied_client_and_task_identity() = runTest {
        val operations = mutableListOf<GraphqlOperation>()
        val client = DownloadsClient(GraphqlClient { operation ->
            operations.add(operation)
            GraphqlResponse(data = when {
                operation.query.contains("createDownload") -> JsonHelper.jsonEncodeToElement(mapOf("createDownload" to task("task"))).jsonObject
                operation.query.contains("downloads") -> JsonHelper.jsonEncodeToElement(mapOf("downloads" to listOf(task("task")))).jsonObject
                else -> JsonHelper.jsonDecode("{}")
            })
        }, backgroundScope)
        assertTrue(client.create(DownloadInput(resources = ResourceDownloadInput(listOf(url), "/target-downloads"))))
        runCurrent()
        val input = JsonHelper.jsonDecodeFromElement<DownloadInput>(operations.first().variables.getValue("input"))
        assertEquals("/target-downloads", input.resources!!.targetDir)
        assertEquals(listOf(url), input.resources!!.urls)
        for (command in DownloadCommand.entries) {
            operations.clear()
            assertTrue(client.control("task", command))
            runCurrent()
            assertEquals(2, operations.size)
            assertTrue(operations.first().query.contains(command.field))
            assertEquals(JsonPrimitive("task"), operations.first().variables["id"])
            assertTrue(operations.last().query.contains("downloads"))
        }
    }

    @Test
    fun command_failure_is_exposed_without_native_fallback() = runTest {
        var requests = 0
        val client = DownloadsClient(GraphqlClient { requests++; error("target unavailable") }, backgroundScope)
        client.control("task", DownloadCommand.CANCEL)
        runCurrent()
        assertEquals(1, requests)
        assertEquals("target unavailable", assertNotNull(client.errors.value).message)
        assertTrue(client.progress.value.isEmpty())
    }

    @Test
    fun disposed_client_rejects_commands_without_sending_them() = runTest {
        var requests = 0
        val scope = CoroutineScope(backgroundScope.coroutineContext + SupervisorJob(backgroundScope.coroutineContext[Job]))
        val client = DownloadsClient(GraphqlClient { requests++; error("Disposed client must not send") }, scope)
        runCurrent()
        scope.cancel()
        runCurrent()
        assertFalse(client.control("task", DownloadCommand.CANCEL))
        runCurrent()
        assertEquals(0, requests)
    }
}
