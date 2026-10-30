package com.ismartcoding.plain.features.files

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ResourceDownloadClientTest {
    @Test
    fun fileIsReturnedOnlyAfterTransferCompletesAndProgressIsRemoved() = runTest {
        val complete = CompletableDeferred<Unit>()
        val client = ResourceDownloadClient { url, name, pin, progress ->
            assertEquals("https://fixture/fs?id=one", url)
            assertEquals("report.pdf", name)
            assertEquals("fingerprint", pin)
            progress(65536, 131072)
            complete.await()
            "/fixture/report.pdf"
        }
        val result = async { client.download("https://fixture/fs?id=one", "report.pdf", "fingerprint") }
        runCurrent()
        assertEquals(65536L, client.progress.value.single().downloadedBytes)
        assertEquals(131072L, client.progress.value.single().totalBytes)
        assertTrue(!result.isCompleted)
        complete.complete(Unit)
        assertEquals("/fixture/report.pdf", result.await())
        assertTrue(client.progress.value.isEmpty())
    }

    @Test
    fun cancellationStopsOnlyTheSelectedTransfer() = runTest {
        val gates = mapOf("one" to CompletableDeferred<Unit>(), "two" to CompletableDeferred<Unit>())
        val cancelled = mutableListOf<String>()
        val client = ResourceDownloadClient { url, _, _, progress ->
            try {
                progress(4, null)
                gates.getValue(url).await()
                "/fixture/$url"
            } catch (error: CancellationException) {
                cancelled += url
                throw error
            }
        }
        val first = async { client.download("one", "one.pdf") }
        val second = async { client.download("two", "two.pdf") }
        runCurrent()
        client.progress.value.single { it.name == "one.pdf" }.cancel()
        assertFailsWith<CancellationException> { first.await() }
        assertEquals(listOf("one"), cancelled)
        assertEquals("two.pdf", client.progress.value.single().name)
        gates.getValue("two").complete(Unit)
        assertEquals("/fixture/two", second.await())
        assertTrue(client.progress.value.isEmpty())
    }

    @Test
    fun failureRemovesProgressAndNeverReturnsAPath() = runTest {
        val client = ResourceDownloadClient { _, _, _, progress ->
            progress(16, 1024)
            error("Fixture truncated response")
        }
        assertFailsWith<IllegalStateException> { client.download("https://fixture/fs", "broken.pdf") }
        assertTrue(client.progress.value.isEmpty())
    }
}
