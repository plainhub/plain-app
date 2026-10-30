package com.ismartcoding.plain.features.shares

import com.ismartcoding.plain.features.downloads.DownloadCenter
import com.ismartcoding.plain.features.downloads.DownloadStatus
import com.ismartcoding.plain.lib.JsonHelper
import com.ismartcoding.plain.models.SharedDownloadRow
import com.ismartcoding.plain.models.SharedDownloadSnapshot
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
class SharedFolderDownloadClientTest {
    private val link = SharedLink("device.test", 8443, "share", "token")
    private val entry = SharedFileDto("target.txt", "/target.txt", size = 8)

    private fun snapshot(runtime: String, revision: Long, id: String) = SharedDownloadSnapshot(
        runtime, revision, listOf(SharedDownloadRow(
            id, "message", ShareBatchType.FILE, "Target download", "/target-downloads", link,
            "url-token", listOf(entry), "", DownloadStatus.DOWNLOADING, "", 4, 8, 4,
            1, 0, 0, entry.name, false, emptyList(),
        )),
    )

    @Test
    fun target_snapshots_are_versioned_and_do_not_publish_into_the_controller_queue() = runTest {
        val local = DownloadCenter.progress.value
        var response = snapshot("target-runtime", 4, "target-task")
        val client = SharedFolderDownloadClient(GraphqlClient {
            GraphqlResponse(data = JsonHelper.jsonEncodeToElement(mapOf("sharedDownloadQueue" to response)).jsonObject)
        }, backgroundScope)
        client.refresh()
        assertEquals(setOf("target-task"), client.progress.value.keys)
        assertEquals(4L, client.progress.value.getValue("target-task").downloadedSize)
        response = snapshot("target-runtime", 3, "stale-task")
        client.refresh()
        assertEquals(setOf("target-task"), client.progress.value.keys)
        response = snapshot("restarted-target", 0, "restarted-task")
        client.refresh()
        assertEquals(setOf("restarted-task"), client.progress.value.keys)
        assertEquals(local, DownloadCenter.progress.value)
    }

    @Test
    fun enqueue_and_every_control_use_the_supplied_target_client() = runTest {
        val operations = mutableListOf<GraphqlOperation>()
        val client = SharedFolderDownloadClient(GraphqlClient { operation ->
            operations.add(operation)
            GraphqlResponse(data = if (operation.query.contains("sharedDownloadQueue")) {
                JsonHelper.jsonEncodeToElement(mapOf("sharedDownloadQueue" to snapshot("target", 1, "task"))).jsonObject
            } else JsonHelper.jsonDecode("{}"))
        }, backgroundScope)
        assertTrue(client.enqueueFile("message", link, "url-token", entry, "/target-downloads"))
        runCurrent()
        val enqueue = operations.first()
        assertTrue(enqueue.query.contains("enqueueSharedDownload"))
        val input = JsonHelper.jsonDecodeFromElement<SharedDownloadInput>(enqueue.variables.getValue("input"))
        assertEquals("/target-downloads", input.targetDir)
        assertEquals(listOf(entry), input.entries)
        for ((command, field) in listOf(
            SharedDownloadCommand.CANCEL to "cancelSharedDownload",
            SharedDownloadCommand.RETRY to "retrySharedDownload",
            SharedDownloadCommand.REMOVE to "removeSharedDownload",
        )) {
            operations.clear()
            assertTrue(client.control("task", command))
            runCurrent()
            assertEquals(2, operations.size)
            assertTrue(operations.first().query.contains(field))
            assertEquals(JsonPrimitive("task"), operations.first().variables["id"])
            assertTrue(operations.last().query.contains("sharedDownloadQueue"))
        }
    }

    @Test
    fun command_failure_is_exposed_without_falling_back_to_native_execution() = runTest {
        var requests = 0
        val client = SharedFolderDownloadClient(GraphqlClient {
            requests++
            error("target unavailable")
        }, backgroundScope)
        client.control("task", SharedDownloadCommand.CANCEL)
        runCurrent()
        assertEquals(1, requests)
        assertEquals("target unavailable", assertNotNull(client.errors.value).message)
        assertTrue(client.progress.value.isEmpty())
    }
    @Test
    fun disposed_client_rejects_commands_without_sending_them() = runTest {
        var requests = 0
        val scope = CoroutineScope(backgroundScope.coroutineContext + SupervisorJob(backgroundScope.coroutineContext[Job]))
        val client = SharedFolderDownloadClient(GraphqlClient {
            requests++
            error("Disposed client must not send")
        }, scope)
        runCurrent()
        scope.cancel()
        runCurrent()
        assertFalse(client.control("task", SharedDownloadCommand.CANCEL))
        runCurrent()
        assertEquals(0, requests)
    }

}
