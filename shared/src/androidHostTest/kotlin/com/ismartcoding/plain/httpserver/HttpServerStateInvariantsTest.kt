package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.TempData
import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.platform.startHttpServerAsync
import com.ismartcoding.plain.platform.stopHttpServerCoreAsync
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
import java.net.ServerSocket
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Regression locks for the 2026-09 "QQ-switch spinner" bug class: the server
 * state must never be stranded in a processing state (STARTING/STOPPING) —
 * the home card maps those to an endless loading spinner. The original bug:
 * STARTING was written as a UI intent before the service even spawned; when
 * the start command was lost (EMUI blocked the foreground-service start as
 * the app went to background), no writer ever followed and the spinner never
 * ended.
 *
 * Invariants locked here:
 * 1. The start orchestrator records a terminal state even when the engine
 *    cannot be created at all (host: no Android context → the create throws).
 *    iOS previously had no catch anywhere and would strand STARTING.
 * 2. The shared stop body lands OFF from any state — the service destroy path
 *    relies on this (onDestroy runs it after cancelling an in-flight start).
 * 3. Concurrent stops serialize on the lifecycle mutex and still land OFF.
 */
class HttpServerStateInvariantsTest {

    @AfterTest
    fun reset() {
        HttpServerManager.httpServerError.value = ""
        HttpServerManager.portsInUse.value = emptySet()
        HttpServerManager.serverState.value = HttpServerState.OFF
    }

    @Test
    fun startOrchestratorRecordsTerminalStateWhenEngineCreateFails() = runBlocking {
        // Free ephemeral ports so the port fallback does not run (it would hit
        // DataStore, unavailable in host tests) — the failure must come from
        // the engine create itself.
        val ports = freePorts(2)
        TempData.httpPort.value = ports[0]
        TempData.httpsPort.value = ports[1]

        startHttpServerAsync()

        assertEquals(
            HttpServerState.ERROR,
            HttpServerManager.serverState.value,
            "an engine-create failure must land ERROR, never strand STARTING (QQ-switch spinner bug)",
        )
        assertTrue(HttpServerManager.httpServerError.value.isNotEmpty(), "failure reason must be recorded for the error card")
    }

    @Test
    fun stopFromStartingLandsOff() = runBlocking {
        // Service destroyed mid-start: the start job is cancelled (STARTING
        // left behind) and the shared stop body must terminal-write OFF.
        HttpServerManager.serverState.value = HttpServerState.STARTING
        stopHttpServerCoreAsync()
        assertEquals(HttpServerState.OFF, HttpServerManager.serverState.value)
    }

    @Test
    fun concurrentStopsSerializeAndLandOff() = runBlocking {
        HttpServerManager.serverState.value = HttpServerState.STARTING
        listOf(
            async { stopHttpServerCoreAsync() },
            async { stopHttpServerCoreAsync() },
        ).awaitAll()
        assertEquals(HttpServerState.OFF, HttpServerManager.serverState.value)
    }

    /** Ephemeral ports verified free right now (bind, read, close). */
    private fun freePorts(n: Int): List<Int> {
        val sockets = (1..n).map { ServerSocket(0) }
        val ports = sockets.map { it.localPort }
        sockets.forEach { it.close() }
        return ports
    }
}
