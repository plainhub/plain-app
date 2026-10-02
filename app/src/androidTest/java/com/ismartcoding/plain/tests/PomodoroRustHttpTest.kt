package com.ismartcoding.plain.tests

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.ismartcoding.plain.features.PomodoroHelper
import com.ismartcoding.plain.ui.page.pomodoro.PomodoroState
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PomodoroRustHttpTest {
    @Test
    fun hostControlsAuthoritativeRustTimerWithoutCompletingARealSession() = runBlocking {
        val original = PomodoroHelper.today()
        assumeTrue(!original.isRunning && !original.isPaused && original.state == PomodoroState.WORK && original.timeLeftSec == original.totalTimeSec)
        try {
            val started = PomodoroHelper.start(30)
            assertTrue(started.isRunning)
            assertFalse(started.isPaused)
            assertTrue(started.timeLeftSec in 1..30)
            val paused = PomodoroHelper.pause()
            assertFalse(paused.isRunning)
            assertTrue(paused.isPaused)
            val (same, completed) = PomodoroHelper.tick()
            assertNull(completed)
            assertEquals(paused.timeLeftSec, same.timeLeftSec)
            assertEquals(original.completedCount, same.completedCount)
            val resumed = PomodoroHelper.start(same.timeLeftSec)
            assertTrue(resumed.isRunning)
            assertFalse(resumed.isPaused)
            assertEquals(original.completedCount, resumed.completedCount)
            val stopped = PomodoroHelper.stop()
            assertFalse(stopped.isRunning)
            assertFalse(stopped.isPaused)
            assertEquals(PomodoroState.WORK, stopped.state)
            assertEquals(stopped.totalTimeSec, stopped.timeLeftSec)
        } finally {
            PomodoroHelper.stop()
        }
    }
}
