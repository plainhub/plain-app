package com.ismartcoding.plain.httpserver

import com.ismartcoding.plain.platform.HttpServerManager

import com.ismartcoding.plain.enums.HttpServerState
import com.ismartcoding.plain.platform.startHttpServerAsync
import com.ismartcoding.plain.platform.onRustHttpServerFailed
import com.ismartcoding.plain.platform.stopHttpServerCoreAsync
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.runBlocking
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
        HttpServerManager.serverState.value = HttpServerState.OFF
    }

    @Test
    fun startOrchestratorRecordsTerminalStateWhenEngineCreateFails() = runBlocking {
        // Native initialization is unavailable in the host environment.
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

    @Test
    fun lateNativeFailureDoesNotChangeStoppedState() = runBlocking {
        HttpServerManager.serverState.value = HttpServerState.OFF
        onRustHttpServerFailed(1L, "late failure")
        assertEquals(HttpServerState.OFF, HttpServerManager.serverState.value)
        assertEquals("", HttpServerManager.httpServerError.value)
    }
}
