package com.ismartcoding.plain

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.ismartcoding.plain.enums.MediaPlayMode
import com.ismartcoding.plain.features.sms.DPendingMms
import kotlinx.coroutines.flow.MutableStateFlow

object TempData {
    val serviceEnabled = MutableStateFlow(false)
    val desktopAccessEnabled = MutableStateFlow(false)
    val webHttps = MutableStateFlow(false)
    val httpPort = MutableStateFlow(8080)
    val httpsPort = MutableStateFlow(8443)
    val dlnaEnabled = MutableStateFlow(false)
    var ip4s = mutableStateOf(emptyList<String>())
    var clientId = ""
    val deviceName = MutableStateFlow("")
    var urlToken = ByteArray(0) // use to encrypt or decrypt params in url (kept as raw bytes to avoid base64 decode on every encrypt/decrypt)
    var mdnsHostname = "plainapp.local" // mDNS hostname for local network discovery

    val audioPlayMode = MutableStateFlow(MediaPlayMode.REPEAT)
    val audioPlaybackSpeed = MutableStateFlow(1f)
    val audioPlayerVisible = MutableStateFlow(false)

    var adbToken = "" // in-memory cache of the ADB automation token

    var nearbyDiscoverable = false
    var developerMode = false

    val awareRunning = MutableStateFlow(false)

    var audioSleepTimerFutureTime = 0L
    var audioPlayPosition = 0L // audio play position in milliseconds

    // mediaId -> playback position in milliseconds; pre-loaded from DB on startup as cache.
    // Mutated from Compose playback callbacks and web/GraphQL routes concurrently, so a
    // plain mutableMapOf (LinkedHashMap) is not safe here.
    val videoPlayProgressMap = mutableStateMapOf<String, Long>()

    // "<mediaType>:<mediaId>" -> duration in seconds; pre-loaded from DB on
    // startup. Used to patch zero-duration MediaStore rows (fMP4 files whose
    // DURATION column is read-only and reports 0). Unit matches DVideo/DAudio.duration.
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
    val pendingMmsMessages = mutableStateListOf<DPendingMms>()

    fun canDesktopAccess(): Boolean {
        return desktopAccessEnabled.value && serviceEnabled.value
    }

    fun canChatAccess(): Boolean {
        return serviceEnabled.value
    }

    fun canDLNAAccess(): Boolean {
        return dlnaEnabled.value && serviceEnabled.value
    }
}
