package com.ismartcoding.plain.ui.nav

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavLoadGateTest {

    @Test
    fun awaitStaysSuspendedWhileGateIsClosed() = runBlocking {
        val gate = NavLoadGate.closed()
        val awaiter = async { gate.await() }
        yieldUntilSettled()
        assertFalse(awaiter.isCompleted, "await must stay suspended while the gate is closed")
        gate.open()
        awaiter.await()
    }

    @Test
    fun awaitPassesImmediatelyOnOpenedGate() = runBlocking {
        val gate = NavLoadGate.opened()
        val awaiter = async { gate.await() }
        yieldUntilSettled()
        assertTrue(awaiter.isCompleted, "await must not suspend on an already-open gate")
    }

    @Test
    fun openIsIdempotent() = runBlocking {
        val gate = NavLoadGate.closed()
        gate.open()
        gate.open()
        assertTrue(gate.isOpen)
        val awaiter = async { gate.await() }
        yieldUntilSettled()
        assertTrue(awaiter.isCompleted)
    }

    @Test
    fun awaitCanBeRepeatedAfterOpen() = runBlocking {
        val gate = NavLoadGate.closed()
        gate.open()
        gate.await()
        gate.await()
    }

    @Test
    fun isOpenReflectsInitialState() {
        assertFalse(NavLoadGate.closed().isOpen)
        assertTrue(NavLoadGate.opened().isOpen)
    }
}

/**
 * Drives the runBlocking event loop enough turns for eager async children
 * to start and park, without any wall-clock waiting.
 */
private suspend fun yieldUntilSettled() {
    repeat(4) { yield() }
}
