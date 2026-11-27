package com.ismartcoding.plain.platform.media

import androidx.media3.common.Player
import java.lang.reflect.Proxy
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AndroidAudioPlayerTest {
    @Test
    fun playAfterEndRestartsTheItemAndIdlePlayPreparesIt() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        var state = Player.STATE_ENDED
        val calls = mutableListOf<String>()
        val player = Proxy.newProxyInstance(Player::class.java.classLoader, arrayOf(Player::class.java)) { _, method, _ ->
            when (method.name) {
                "getPlaybackState" -> state
                "isCommandAvailable", "getPlayWhenReady" -> true
                "getPlaybackSuppressionReason" -> Player.PLAYBACK_SUPPRESSION_REASON_NONE
                else -> { calls += method.name; null }
            }
        } as Player
        val field = AndroidAudioPlayer::class.java.getDeclaredField("player").apply { isAccessible = true }
        val previous = field.get(AndroidAudioPlayer)
        try {
            field.set(AndroidAudioPlayer, player)
            AndroidAudioPlayer.play()
            assertEquals(listOf("seekToDefaultPosition", "play"), calls)
            state = Player.STATE_IDLE
            calls.clear()
            AndroidAudioPlayer.play()
            assertEquals(listOf("prepare", "play"), calls)
        } finally {
            field.set(AndroidAudioPlayer, previous)
            Dispatchers.resetMain()
        }
    }
}
