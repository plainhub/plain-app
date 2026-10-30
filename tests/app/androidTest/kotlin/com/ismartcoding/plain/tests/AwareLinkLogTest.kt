package com.ismartcoding.plain.tests

import android.os.Build
import android.os.Process
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.connectivityManager
import com.ismartcoding.plain.platform.network.AwarePeer
import com.ismartcoding.plain.platform.network.aware.AwarePeerLink
import com.ismartcoding.plain.platform.network.aware.AwareSession
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class AwareLinkLogTest {
    @Test
    fun cancellingPendingBuildRestoresIdleWithoutFailureWarnings() = runBlocking {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        val peer = AwarePeer("aware-log-${UUID.randomUUID()}", 1025)
        val link = AwarePeerLink(peer.id, AwareSession(), connectivityManager, true, null, 1024) { _, _ -> }
        try {
            val pending = async(start = CoroutineStart.UNDISPATCHED) { link.build(peer) }
            assertTrue(link.isBuilding())
            pending.cancelAndJoin()
            assertTrue(pending.isCancelled)
            assertEquals("IDLE", link.stateName())
            val reader = ProcessBuilder("logcat", "-d", "--pid=${Process.myPid()}", "-v", "brief").start()
            val output = reader.inputStream.bufferedReader().use { it.readText() }
            assertEquals(0, reader.waitFor())
            assertTrue("Build diagnostic must be present", output.contains("build start peer=${peer.id}"))
            assertTrue("Cancellation must not be reported as a failure", output.lineSequence().none {
                it.contains(peer.id) && (it.startsWith("W/") || it.startsWith("E/"))
            })
        } finally {
            link.close("test complete")
        }
    }

    @Test
    fun connectionAttemptTimeoutRemainsAnOrdinaryFailure() = runBlocking {
        assumeTrue(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
        val peer = AwarePeer("aware-timeout-${UUID.randomUUID()}", 1025)
        val link = AwarePeerLink(peer.id, AwareSession(), connectivityManager, true, null, 1024) { _, _ -> }
        try {
            val failure = withTimeout(7_000) { runCatching { link.build(peer) }.exceptionOrNull() }
            assertTrue("Attempt timeout must reach the failure handler", failure is IllegalStateException && failure !is CancellationException)
            assertEquals("Wi-Fi Aware connection attempt timed out", failure?.message)
            assertEquals("IDLE", link.stateName())
        } finally {
            link.close("test complete")
        }
    }
}
