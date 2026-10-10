package com.ismartcoding.plain

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.ismartcoding.plain.preferences.*
import kotlinx.coroutines.flow.MutableStateFlow

object TempData {

    var ip4s = mutableStateOf(emptyList<String>())
    val clientId: String get() = RustSystemState.state.value.clientId
    val deviceName = MutableStateFlow("")
    val urlToken: ByteArray get() = kotlin.io.encoding.Base64.decode(RustSystemState.state.value.urlToken)
    val mdnsHostname: String get() = RustSystemState.state.value.mdnsHostname

    val audioPlayerVisible = MutableStateFlow(false)
    /** Media path requested by a home-screen shortcut; null = no preview open. */
    val shortcutMediaPath = MutableStateFlow<String?>(null)




    val awareRunning = MutableStateFlow(false)

    var audioSleepTimerFutureTime = 0L
    var audioPlayPosition = 0L // audio play position in milliseconds

    // mediaId -> playback position in milliseconds; pre-loaded from DB on startup as cache.
    // Mutated from Compose playback callbacks and web/GraphQL routes concurrently, so a
    // plain mutableMapOf (LinkedHashMap) is not safe here.
    val videoPlayProgressMap = mutableStateMapOf<String, Long>()

    // "<mediaType>:<mediaId>" -> duration in milliseconds; pre-loaded from DB on
    // startup. Used to patch zero-duration MediaStore rows (fMP4 files whose
    // DURATION column is read-only and reports 0). Unit matches DVideo/DAudio.durationMs.
    val mediaDurationMap = mutableStateMapOf<String, Long>()

    // Encoded target id of the chat page currently in the foreground. Set by
    // ChatPageEffects so the chat receiver can suppress notifications for the
    // active conversation. Format: "peer:<id>" / "channel:<id>" / "local".
    var activeToId = ""

    /**
     * MMS messages that have been launched in the default SMS app but not yet
     * confirmed as sent.  Exposed through the sms query so the web can show a
     * "sending…" state before and after a page refresh.
     */

    fun canDesktopAccess(): Boolean {
        return UserPrefs.desktopAccess.value
    }

    fun canChatAccess(): Boolean {
        return com.ismartcoding.plain.platform.HttpServerManager.serverState.value == com.ismartcoding.plain.enums.HttpServerState.ON
    }

    fun canDLNAAccess(): Boolean {
        return UserPrefs.dlna.value
    }
}
