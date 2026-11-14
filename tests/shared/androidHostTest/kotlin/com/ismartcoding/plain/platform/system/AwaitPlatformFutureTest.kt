package com.ismartcoding.plain.platform.system

import com.google.common.util.concurrent.SettableFuture
import java.util.concurrent.Executor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest

class AwaitPlatformFutureTest {
    private class PendingExecutor : Executor {
        private val tasks = ArrayDeque<Runnable>()
        override fun execute(command: Runnable) { tasks.addLast(command) }
        fun drain() { while (tasks.isNotEmpty()) tasks.removeFirst().run() }
    }

    @Test
    fun returns_connected_resource_without_disposing_it() = runTest {
        val future = SettableFuture.create<Any>()
        val executor = PendingExecutor()
        var releases = 0
        val result = async(start = CoroutineStart.UNDISPATCHED) { awaitPlatformFuture(future, executor) { releases++ } }
        val resource = Any()
        future.set(resource)
        executor.drain()
        assertSame(resource, result.await())
        assertEquals(0, releases)
    }

    @Test
    fun failure_reaches_waiter_with_original_cause() = runTest {
        supervisorScope {
            val future = SettableFuture.create<Any>()
            val executor = PendingExecutor()
            val failure = IllegalStateException("connection failed")
            val result = async(start = CoroutineStart.UNDISPATCHED) { awaitPlatformFuture(future, executor) {} }
            future.setException(failure)
            executor.drain()
            val actual = try { result.await(); null } catch (error: IllegalStateException) { error }
            assertEquals(failure.message, actual?.message)
            assertTrue(generateSequence(actual as Throwable?) { it.cause }.any { it === failure })
        }
    }

    @Test
    fun cancellation_cancels_pending_connection() = runTest {
        val future = SettableFuture.create<Any>()
        val executor = PendingExecutor()
        val result = async(start = CoroutineStart.UNDISPATCHED) { awaitPlatformFuture(future, executor) {} }
        result.cancel()
        executor.drain()
        result.join()
        assertTrue(future.isCancelled)
    }

    @Test
    fun canceled_waiter_releases_connected_resource_before_delivery() = runTest {
        val future = SettableFuture.create<Any>()
        val executor = PendingExecutor()
        var releases = 0
        val result = async(start = CoroutineStart.UNDISPATCHED) { awaitPlatformFuture(future, executor) { releases++ } }
        future.set(Any())
        result.cancel()
        executor.drain()
        result.join()
        executor.drain()
        assertEquals(1, releases)
    }

    @Test
    fun cancellation_after_callback_before_resume_releases_resource_once() = runTest {
        val future = SettableFuture.create<Any>()
        val executor = PendingExecutor()
        var releases = 0
        val result = async(start = CoroutineStart.UNDISPATCHED) { awaitPlatformFuture(future, executor) { releases++ } }
        future.set(Any())
        executor.drain()
        result.cancel()
        runCurrent()
        result.join()
        executor.drain()
        assertEquals(1, releases)
    }
}
