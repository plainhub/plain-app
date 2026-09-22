package com.ismartcoding.plain.services

import com.ismartcoding.plain.events.KeepAwakeChangedEvent
import com.ismartcoding.plain.events.PowerConnectedEvent
import com.ismartcoding.plain.events.PowerDisconnectedEvent
import com.ismartcoding.plain.events.WebRequestReceivedEvent
import com.ismartcoding.plain.events.WindowFocusChangedEvent
import com.ismartcoding.plain.lib.ChannelEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class HttpServerLockManagerTest {
    private class Fixture {
        val held = AtomicBoolean(false)
        val now = AtomicLong(0)
        var usb = false
        val events = MutableSharedFlow<ChannelEvent>()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val manager = HttpServerLockManager(
            setLockHeld = { held.set(it) },
            isUSBConnected = { usb },
            readKeepAwake = { false },
            clock = { now.get() },
            events = events,
            scope = scope,
            checkIntervalMs = 5,
            inactivityTimeoutMs = 100,
        )

        suspend fun expire() {
            now.addAndGet(101)
            withTimeout(2000) { while (held.get()) delay(5) }
        }

        fun close() { manager.stop(); scope.cancel() }
    }

    @Test
    fun enablingKeepAwakeReacquiresAnExpiredLock() = runBlocking {
        val f = Fixture()
        try {
            f.manager.start()
            assertTrue(f.held.get())
            f.expire()
            f.events.emit(KeepAwakeChangedEvent(true))
            assertTrue(f.held.get(), "Keep awake must reacquire, not only cancel the idle timer")
            f.now.addAndGet(1000)
            delay(20)
            assertTrue(f.held.get())
        } finally { f.close() }
    }

    @Test
    fun authenticatedRequestReacquiresAnExpiredLock() = runBlocking {
        val f = Fixture()
        try {
            f.manager.start()
            f.expire()
            f.events.emit(WebRequestReceivedEvent())
            assertTrue(f.held.get(), "An active client must get a fresh idle window")
            f.expire()
            assertFalse(f.held.get())
        } finally { f.close() }
    }

    @Test
    fun foregroundAndPowerReacquireButExplicitStopRemainsTerminal() = runBlocking {
        val f = Fixture()
        try {
            f.manager.start()
            f.expire()
            f.events.emit(WindowFocusChangedEvent(true))
            assertTrue(f.held.get())
            f.expire()
            f.usb = true
            f.events.emit(PowerConnectedEvent())
            assertTrue(f.held.get())
            f.now.addAndGet(1000)
            delay(20)
            assertTrue(f.held.get())
            f.usb = false
            f.events.emit(PowerDisconnectedEvent())
            f.expire()
            f.manager.stop()
            f.events.emit(KeepAwakeChangedEvent(true))
            f.events.emit(WebRequestReceivedEvent())
            assertFalse(f.held.get())
        } finally { f.close() }
    }

    @Test
    fun repeatedStartDoesNotLeaveACollectorAliveAfterStop() = runBlocking {
        val f = Fixture()
        try {
            f.manager.start()
            f.manager.start()
            f.manager.stop()
            f.events.emit(WindowFocusChangedEvent(true))
            assertFalse(f.held.get(), "A duplicate start must not leak a collector past stop")
        } finally { f.close() }
    }

    @Test
    fun requestsSlideIdleWindowAndDisablingKeepAwakeStartsAFreshWindow() = runBlocking {
        val f = Fixture()
        try {
            f.manager.start()
            f.now.set(90)
            f.events.emit(WebRequestReceivedEvent())
            f.now.set(150)
            delay(20)
            assertTrue(f.held.get())
            f.events.emit(KeepAwakeChangedEvent(true))
            f.now.set(1000)
            f.events.emit(KeepAwakeChangedEvent(false))
            delay(20)
            assertTrue(f.held.get())
            f.expire()
            assertFalse(f.held.get())
        } finally { f.close() }
    }

    @Test
    fun latePreferenceReadFromStoppedGenerationCannotChangeRestartedManager() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
        val gate = CompletableDeferred<Boolean>()
        val held = AtomicBoolean(false)
        val now = AtomicLong(0)
        var reads = 0
        val manager = HttpServerLockManager(
            setLockHeld = { held.set(it) },
            isUSBConnected = { false },
            readKeepAwake = {
                if (++reads == 1) withContext(NonCancellable) { gate.await() } else true
            },
            clock = { now.get() },
            events = MutableSharedFlow(),
            scope = scope,
            checkIntervalMs = 5,
            inactivityTimeoutMs = 100,
        )
        try {
            manager.start()
            manager.stop()
            manager.start()
            gate.complete(false)
            now.set(1000)
            delay(30)
            assertTrue(held.get())
            manager.stop()
            assertFalse(held.get())
        } finally {
            gate.complete(false)
            manager.stop()
            scope.cancel()
        }
    }
}
