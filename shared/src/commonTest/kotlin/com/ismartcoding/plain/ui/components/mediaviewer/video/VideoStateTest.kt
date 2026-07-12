package com.ismartcoding.plain.ui.components.mediaviewer.video

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * A recording fake implementation of [VideoPlayerController] for testing [VideoState].
 * Every call is recorded so assertions can verify delegation.
 */
private class FakeVideoPlayerController : VideoPlayerController {
    var playCount = 0
    var pauseCount = 0
    var stopCount = 0
    var prepareCount = 0
    var seekPositions = mutableListOf<Long>()
    var speeds = mutableListOf<Float>()
    var mutedValues = mutableListOf<Boolean>()
    var released = false
    var mediaItems = mutableListOf<String>()
    var focusRequested = false
    var focusAbandoned = false
    var listener: ((VideoPlayerEvent) -> Unit)? = null

    // Configurable state for updateTime() to read
    var fakeIsPlaying = false
    var fakeCurrentPosition = 0L
    var fakeDuration = 0L
    var fakeBufferedPercentage = 0

    override fun play() { playCount++ }
    override fun pause() { pauseCount++ }
    override fun stop() { stopCount++ }
    override fun prepare() { prepareCount++ }
    override fun seekTo(positionMs: Long) { seekPositions.add(positionMs) }
    override fun setPlaybackSpeed(speed: Float) { speeds.add(speed) }
    override fun setMuted(muted: Boolean) { mutedValues.add(muted) }
    override fun release() { released = true }
    override fun setMediaItem(path: String) { mediaItems.add(path) }
    override fun setEventListener(listener: (VideoPlayerEvent) -> Unit) { this.listener = listener }
    override fun requestAudioFocus() { focusRequested = true }
    override fun abandonAudioFocus() { focusAbandoned = true }

    override val duration: Long get() = fakeDuration
    override val currentPosition: Long get() = fakeCurrentPosition
    override val bufferedPercentage: Int get() = fakeBufferedPercentage
    override val isPlaying: Boolean get() = fakeIsPlaying
}

class VideoStateTest {

    @Test
    fun `initData sets controller and applies current speed`() {
        val state = VideoState()
        val fake = FakeVideoPlayerController()

        state.initData(fake)

        assertEquals(fake, state.controller)
        assertEquals(listOf(1f), fake.speeds)
    }

    @Test
    fun `initData applies mute when already muted`() {
        val state = VideoState().apply { isMuted = true }
        val fake = FakeVideoPlayerController()

        state.initData(fake)

        assertEquals(listOf(true), fake.mutedValues)
        assertEquals(listOf(1f), fake.speeds)
    }

    @Test
    fun `initData does not mute when not muted`() {
        val state = VideoState()
        val fake = FakeVideoPlayerController()

        state.initData(fake)

        assertTrue(fake.mutedValues.isEmpty())
    }

    @Test
    fun `changeSpeed updates state and delegates to controller`() {
        val state = VideoState()
        val fake = FakeVideoPlayerController()
        state.initData(fake)

        state.changeSpeed(1.5f)

        assertEquals(1.5f, state.speed)
        assertEquals(listOf(1f, 1.5f), fake.speeds)
    }

    @Test
    fun `changeSpeed does nothing when controller is null`() {
        val state = VideoState()

        state.changeSpeed(2f)

        assertEquals(2f, state.speed)
    }

    @Test
    fun `startSpeedBoost activates 2x and sets flag`() {
        val state = VideoState()
        val fake = FakeVideoPlayerController()
        state.initData(fake)

        state.startSpeedBoost()

        assertTrue(state.isSpeedBoostActive)
        assertEquals(listOf(1f, 2f), fake.speeds)
    }

    @Test
    fun `startSpeedBoost is idempotent`() {
        val state = VideoState()
        val fake = FakeVideoPlayerController()
        state.initData(fake)

        state.startSpeedBoost()
        state.startSpeedBoost()

        assertTrue(state.isSpeedBoostActive)
        // Only one 2f call — second call is a no-op
        assertEquals(listOf(1f, 2f), fake.speeds)
    }

    @Test
    fun `stopSpeedBoost restores original speed and clears flag`() {
        val state = VideoState()
        val fake = FakeVideoPlayerController()
        state.initData(fake)
        state.changeSpeed(1.5f)
        state.startSpeedBoost()

        state.stopSpeedBoost()

        assertFalse(state.isSpeedBoostActive)
        // initData(1f), changeSpeed(1.5f), startBoost(2f), stopBoost(1.5f)
        assertEquals(listOf(1f, 1.5f, 2f, 1.5f), fake.speeds)
    }

    @Test
    fun `stopSpeedBoost is no-op when not active`() {
        val state = VideoState()
        val fake = FakeVideoPlayerController()
        state.initData(fake)

        state.stopSpeedBoost()

        assertFalse(state.isSpeedBoostActive)
        assertEquals(listOf(1f), fake.speeds)
    }

    @Test
    fun `seekTo sets seeking flag updates currentTime and delegates`() {
        val state = VideoState()
        val fake = FakeVideoPlayerController()
        state.initData(fake)
        state.isPlaying = true // already playing — should NOT call play()

        state.seekTo(5000L)

        assertTrue(state.isSeeking)
        assertEquals(5000L, state.currentTime)
        assertEquals(listOf(5000L), fake.seekPositions)
        assertEquals(0, fake.playCount)
    }

    @Test
    fun `seekTo auto-plays when not playing`() {
        val state = VideoState()
        val fake = FakeVideoPlayerController()
        state.initData(fake)
        state.isPlaying = false

        state.seekTo(3000L)

        assertEquals(1, fake.playCount)
    }

    @Test
    fun `togglePlay pauses when playing`() {
        val state = VideoState()
        val fake = FakeVideoPlayerController()
        state.initData(fake)
        state.isPlaying = true

        state.togglePlay()

        assertEquals(1, fake.pauseCount)
        assertEquals(0, fake.playCount)
    }

    @Test
    fun `togglePlay plays when paused`() {
        val state = VideoState()
        val fake = FakeVideoPlayerController()
        state.initData(fake)
        state.isPlaying = false
        state.currentTime = 1000L
        state.totalTime = 5000L

        state.togglePlay()

        assertEquals(1, fake.playCount)
        assertEquals(0, fake.pauseCount)
    }

    @Test
    fun `togglePlay restarts from beginning when at end`() {
        val state = VideoState()
        val fake = FakeVideoPlayerController()
        state.initData(fake)
        state.isPlaying = false
        state.currentTime = 5000L
        state.totalTime = 5000L

        state.togglePlay()

        assertEquals(0L, state.currentTime)
        assertEquals(listOf(0L), fake.seekPositions)
        assertEquals(1, fake.playCount)
    }

    @Test
    fun `toggleMute mutes when unmuted`() {
        val state = VideoState()
        val fake = FakeVideoPlayerController()
        state.initData(fake)

        state.toggleMute()

        assertTrue(state.isMuted)
        assertEquals(listOf(true), fake.mutedValues)
    }

    @Test
    fun `toggleMute unmutes when muted`() {
        val state = VideoState()
        val fake = FakeVideoPlayerController()
        state.initData(fake)
        state.isMuted = true

        state.toggleMute()

        assertFalse(state.isMuted)
        assertEquals(listOf(false), fake.mutedValues)
    }

    @Test
    fun `updateTime reads controller state`() {
        val state = VideoState()
        val fake = FakeVideoPlayerController()
        state.initData(fake)
        fake.fakeIsPlaying = true
        fake.fakeCurrentPosition = 2000L
        fake.fakeDuration = 10000L
        fake.fakeBufferedPercentage = 75

        state.updateTime()

        assertTrue(state.isPlaying)
        assertEquals(2000L, state.currentTime)
        assertEquals(10000L, state.totalTime)
        assertEquals(75, state.bufferedPercentage)
    }

    @Test
    fun `updateTime is skipped when seeking`() {
        val state = VideoState()
        val fake = FakeVideoPlayerController()
        state.initData(fake)
        state.isSeeking = true
        state.currentTime = 9999L
        fake.fakeCurrentPosition = 2000L

        state.updateTime()

        // currentTime unchanged because isSeeking
        assertEquals(9999L, state.currentTime)
    }

    @Test
    fun `updateTime is no-op when controller is null`() {
        val state = VideoState()

        state.updateTime()

        assertEquals(0L, state.currentTime)
        assertEquals(0L, state.totalTime)
    }

    @Test
    fun `updateTime coerces negative position to zero`() {
        val state = VideoState()
        val fake = FakeVideoPlayerController()
        state.initData(fake)
        fake.fakeCurrentPosition = -100L
        fake.fakeDuration = -50L

        state.updateTime()

        assertEquals(0L, state.currentTime)
        assertEquals(0L, state.totalTime)
    }

    @Test
    fun `togglePlay does nothing when controller is null`() {
        val state = VideoState()
        state.isPlaying = true

        state.togglePlay()

        // togglePlay only delegates to controller; isPlaying is not flipped locally
        assertTrue(state.isPlaying)
    }

    @Test
    fun `toggleMute does nothing when controller is null`() {
        val state = VideoState()

        state.toggleMute()

        // isMuted is toggled locally even without controller
        assertTrue(state.isMuted)
    }

    @Test
    fun `seekTo does nothing when controller is null`() {
        val state = VideoState()

        state.seekTo(5000L)

        // isSeeking and currentTime are set locally even without controller
        assertTrue(state.isSeeking)
        assertEquals(5000L, state.currentTime)
    }
}
